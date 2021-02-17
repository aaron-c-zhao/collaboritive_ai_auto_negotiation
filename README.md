# Group 42 Negotiation Assignment

## Glossary

* SAOP: each agent can only respond to the most recent offer in its own turn. The turn taking sequences are predefined.

## FAQ

### How to debug the party in Intellij?

1. Clone the simplerunner repo and import it in Intellij
2. Open 'Project Structure' dialog, navi to 'modules' section and add the party repo as a module of the Project.
    * Choose 'Import Module' 
        ![import module](./images/import_module.png "Step1: import module")
    * Find the party directory and import it as an external model
        ![external_module](./images/external_module.png)
    * Intellij should recognise and add the newly added module as a dependency when the POM is configured correctly.
3. Run the simplerunner in debug mode, it will stop at the bps set in the party module.
   