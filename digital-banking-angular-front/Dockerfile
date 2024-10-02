# Use a Node.js base image
FROM node:16-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the Angular project into the container
COPY . /app

# Install project dependencies and build the Angular app
RUN npm install
RUN npm run build --prod

# Use a base Nginx image to serve the Angular app
FROM nginx:alpine
COPY --from=0 /app/dist/digital-banking-angular-front /usr/share/nginx/html

# Expose the port the Angular frontend will run on
EXPOSE 80
