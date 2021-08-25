Challenge Account Management

This Challenge is just a prototype and to make it a product ready to be delivered some improvement can be done.
Here are some examples:

- Integration with a real database, the project is using a mock database ("map") to save locally the information, 
the goal should be to create a connection with an existing database in order that the repository could access it;

- Creation of a CD/CI methodology to control the app developments and automation;

- Implement a log trace mechanism, in order to track and catch future problems;

- Implement an audit of all requests made by the users (very helpful to find problems/information);

- Make always use of the TDD (test-driven development), the developments should always follow the previously written tests;

- App and database containerization using docker;

- Improve controllers exception handling using a global exception handler;

- In the future addition of functionalities always follow the microservice architecture.
