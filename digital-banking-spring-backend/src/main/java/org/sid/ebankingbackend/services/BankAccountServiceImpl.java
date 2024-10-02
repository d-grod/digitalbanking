package org.sid.ebankingbackend.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sid.ebankingbackend.dtos.*;
import org.sid.ebankingbackend.entities.*;
import org.sid.ebankingbackend.enums.OperationType;
import org.sid.ebankingbackend.exceptions.BalanceNotSufficientException;
import org.sid.ebankingbackend.exceptions.BankAccountNotFoundException;
import org.sid.ebankingbackend.exceptions.CustomerNotFoundException;
import org.sid.ebankingbackend.mappers.BankAccountMapperImpl;
import org.sid.ebankingbackend.repositories.AccountOperationRepository;
import org.sid.ebankingbackend.repositories.BankAccountRepository;
import org.sid.ebankingbackend.repositories.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class BankAccountServiceImpl implements BankAccountService {
    private CustomerRepository customerRepository;
    private BankAccountRepository bankAccountRepository;
    private AccountOperationRepository accountOperationRepository;
    private BankAccountMapperImpl dtoMapper;

    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDTO) {
        log.info("Saving new Customer");
        Customer customer=dtoMapper.fromCustomerDTO(customerDTO);
        Customer savedCustomer = customerRepository.save(customer);
        return dtoMapper.fromCustomer(savedCustomer);
    }

    @Override
    public CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overDraft, Long customerId) throws CustomerNotFoundException {
        Customer customer=customerRepository.findById(customerId).orElse(null);
        if(customer==null)
            throw new CustomerNotFoundException("Customer not found");
        CurrentAccount currentAccount=new CurrentAccount();
        currentAccount.setId(UUID.randomUUID().toString());
        currentAccount.setCreatedAt(new Date());
        currentAccount.setBalance(initialBalance);
        currentAccount.setOverDraft(overDraft);
        currentAccount.setCustomer(customer);
        CurrentAccount savedBankAccount = bankAccountRepository.save(currentAccount);
        return dtoMapper.fromCurrentBankAccount(savedBankAccount);
    }

    @Override
    public SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId) throws CustomerNotFoundException {
        Customer customer=customerRepository.findById(customerId).orElse(null);
        if(customer==null)
            throw new CustomerNotFoundException("Customer not found");
        SavingAccount savingAccount=new SavingAccount();
        savingAccount.setId(UUID.randomUUID().toString());
        savingAccount.setCreatedAt(new Date());
        savingAccount.setBalance(initialBalance);
        savingAccount.setInterestRate(interestRate);
        savingAccount.setCustomer(customer);
        SavingAccount savedBankAccount = bankAccountRepository.save(savingAccount);
        return dtoMapper.fromSavingBankAccount(savedBankAccount);
    }

    @Override
    public List<CustomerDTO> listCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(customer -> dtoMapper.fromCustomer(customer))
                .collect(Collectors.toList());
    }

    @Override
    public BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFoundException {
        BankAccount bankAccount=bankAccountRepository.findById(accountId)
                .orElseThrow(()->new BankAccountNotFoundException("BankAccount not found"));
        if(bankAccount instanceof SavingAccount){
            SavingAccount savingAccount= (SavingAccount) bankAccount;
            return dtoMapper.fromSavingBankAccount(savingAccount);
        } else {
            CurrentAccount currentAccount= (CurrentAccount) bankAccount;
            return dtoMapper.fromCurrentBankAccount(currentAccount);
        }
    }

    //TODO: IMPLEMENT THIS
    @Override
    public void debit(String accountId, double amount, String description) throws BankAccountNotFoundException, BalanceNotSufficientException {
        System.out.println("############################################# DEBIT ######################");
        // Récupérer le compte en banque par son ID
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("Compte non trouvé pour ID: " + accountId));

        // Vérifier si le solde est suffisant
        if (bankAccount.getBalance() < amount) {
            throw new BalanceNotSufficientException("Solde insuffisant pour débiter le compte ID: " + accountId);
        }

        // Débiter le montant
        bankAccount.setBalance(bankAccount.getBalance() - amount);

        // Créer une opération de compte
        AccountOperation operation = new AccountOperation();
        operation.setBankAccount(bankAccount);
        operation.setAmount(amount); // Montant débité
        operation.setDescription(description);
        operation.setOperationDate(new Date());
        operation.setType(OperationType.DEBIT);

        // Ajouter l'opération à la liste d'opérations
        bankAccount.getAccountOperations().add(operation);

        // Enregistrer les changements (cela peut être un appel à la méthode save de votre repository)
        bankAccountRepository.save(bankAccount);
        accountOperationRepository.save(operation);
    }



    //TODO: IMPLEMENT THIS
    @Override
    public void credit(String accountId, double amount, String description) throws BankAccountNotFoundException {
        // Étape 1 : Rechercher le compte bancaire
        System.out.println("############################################# CREDIT ######################");
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("Compte non trouvé"));

        // Étape 2 : Mettre à jour le solde
        account.setBalance(account.getBalance() + amount);

        // Étape 3 : Créer une opération comptable
        AccountOperation operation = new AccountOperation();
        operation.setAmount(amount);
        operation.setDescription(description);
        operation.setOperationDate(new Date());
        operation.setBankAccount(account);
        operation.setType(OperationType.CREDIT);

        // Enregistrer l'opération dans le repository
        accountOperationRepository.save(operation);

        // Étape 4 : Sauvegarder le compte mis à jour
        bankAccountRepository.save(account);
    }



    //TODO: IMPLEMENT THIS
    @Override
    public void transfer(String accountIdSource, String accountIdDestination, double amount) throws BankAccountNotFoundException, BalanceNotSufficientException {

        // Récupérer le compte source
        BankAccount sourceAccount = bankAccountRepository.findById(accountIdSource)
                .orElseThrow(() -> new BankAccountNotFoundException("Compte source non trouvé pour ID: " + accountIdSource));

        // Récupérer le compte destination
        BankAccount destinationAccount = bankAccountRepository.findById(accountIdDestination)
                .orElseThrow(() -> new BankAccountNotFoundException("Compte destination non trouvé pour ID: " + accountIdDestination));

        // Vérifier si le solde du compte source est suffisant
        if (sourceAccount.getBalance() < amount) {
            throw new BalanceNotSufficientException("Solde insuffisant pour transférer depuis le compte ID: " + accountIdSource);
        }

        // Débiter le compte source
        sourceAccount.setBalance(sourceAccount.getBalance() - amount);

        // Créer une opération pour le compte source
        AccountOperation debitOperation = new AccountOperation();
        debitOperation.setBankAccount(sourceAccount);
        debitOperation.setAmount(amount); // Montant débité
        debitOperation.setDescription("Transfert vers " + accountIdDestination);
        debitOperation.setOperationDate(new Date());
        debitOperation.setType(OperationType.DEBIT);

        // Ajouter l'opération à la liste d'opérations du compte source
        sourceAccount.getAccountOperations().add(debitOperation);

        // Créditer le compte destination
        destinationAccount.setBalance(destinationAccount.getBalance() + amount);

        // Créer une opération pour le compte destination
        AccountOperation creditOperation = new AccountOperation();
        creditOperation.setBankAccount(destinationAccount);
        creditOperation.setAmount(amount); // Montant crédité
        creditOperation.setDescription("Transfert depuis " + accountIdSource);
        creditOperation.setOperationDate(new Date());
        creditOperation.setType(OperationType.CREDIT);

        // Ajouter l'opération à la liste d'opérations du compte destination
        destinationAccount.getAccountOperations().add(creditOperation);

        // Enregistrer les changements dans la base de données
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);
        accountOperationRepository.save(debitOperation);
        accountOperationRepository.save(creditOperation);
    }



    @Override
    public List<BankAccountDTO> bankAccountList(){
        List<BankAccount> bankAccounts = bankAccountRepository.findAll();
        List<BankAccountDTO> bankAccountDTOS = bankAccounts.stream().map(bankAccount -> {
            if (bankAccount instanceof SavingAccount) {
                SavingAccount savingAccount = (SavingAccount) bankAccount;
                return dtoMapper.fromSavingBankAccount(savingAccount);
            } else {
                CurrentAccount currentAccount = (CurrentAccount) bankAccount;
                return dtoMapper.fromCurrentBankAccount(currentAccount);
            }
        }).collect(Collectors.toList());
        return bankAccountDTOS;
    }
    @Override
    public CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer Not found"));
        return dtoMapper.fromCustomer(customer);
    }

    @Override
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        log.info("Saving new Customer");
        Customer customer=dtoMapper.fromCustomerDTO(customerDTO);
        Customer savedCustomer = customerRepository.save(customer);
        return dtoMapper.fromCustomer(savedCustomer);
    }
    @Override
    public void deleteCustomer(Long customerId){
        customerRepository.deleteById(customerId);
    }
    @Override
    public List<AccountOperationDTO> accountHistory(String accountId){
        List<AccountOperation> accountOperations = accountOperationRepository.findByBankAccountId(accountId);
        return accountOperations.stream().map(op->dtoMapper.fromAccountOperation(op)).collect(Collectors.toList());
    }

    @Override
    public AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException {
        BankAccount bankAccount=bankAccountRepository.findById(accountId).orElse(null);
        if(bankAccount==null) throw new BankAccountNotFoundException("Account not Found");
        Page<AccountOperation> accountOperations = accountOperationRepository.findByBankAccountIdOrderByOperationDateDesc(accountId, PageRequest.of(page, size));
        AccountHistoryDTO accountHistoryDTO=new AccountHistoryDTO();
        List<AccountOperationDTO> accountOperationDTOS = accountOperations.getContent().stream().map(op -> dtoMapper.fromAccountOperation(op)).collect(Collectors.toList());
        accountHistoryDTO.setAccountOperationDTOS(accountOperationDTOS);
        accountHistoryDTO.setAccountId(bankAccount.getId());
        accountHistoryDTO.setBalance(bankAccount.getBalance());
        accountHistoryDTO.setCurrentPage(page);
        accountHistoryDTO.setPageSize(size);
        accountHistoryDTO.setTotalPages(accountOperations.getTotalPages());
        return accountHistoryDTO;
    }

    @Override
    public List<CustomerDTO> searchCustomers(String keyword) {
        List<Customer> customers=customerRepository.searchCustomer(keyword);
        List<CustomerDTO> customerDTOS = customers.stream().map(cust -> dtoMapper.fromCustomer(cust)).collect(Collectors.toList());
        return customerDTOS;
    }
}
