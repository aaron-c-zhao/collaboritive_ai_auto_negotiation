# Group 42 Negotiation Assignment

## Glossary

* SAOP: each agent can only respond to the most recent offer in its own turn. The turn taking sequences are predefined.

## Description of components

### Bidding Strategy

Based on time dependent bidding strategy like Boulware or conceder, inspired by Hard-headed,
the bidding strategy of agent group42 is composed as following:
1. In the first phase of the bidding, it behaves like a conceder which concedes at a constant rate util
the utility reaches 0.9. The purpose of this phase is to learn the opponent's behavior pattern.
   
2. In the second phase, based on the 10 most recent opponent's bids, by applying linear regression analysis,
the agent tries to calculate the 'niceness' of opponent. The niceness is a coefficient that is normalised to [0, 1], 
which entails how tough is the opponent playing.
   
3. After the niceness has been retrieved, it's plugged in to the following formula: 
f(t) = 0.9 - (t - (0.05 + 0.2 * niceness)) ^ 3.0, where t is the progress. Thus, the niceness of the opponent affects
the inflection point of the concession curve. 
   
4. In the third phase, instead of using the niceness to decide when to concede, the niceness is used to 
decide how fast it concedes. The influence can be seen in the following pictures:
   
![time function](./images/concession_curve_1.png)
   
![time_function](./images/concession_curve_2.png)


5. The last phase covers only the last round. If no agreement has been achieved, then the agent will accept anything
above the reservation value.
   
6. The strategy plays tough against nice opponent, and play nicer against really tough opponent. It's intention is to 
secure as many agreements as possible with possibly higher utility. 

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
   
### How to use BOA as a dependency?

1. install (copy and paste) the folder [/boa](jars) into your local maven .m2 repository. 
 * For windows user, .m2 should be under the path C:/Users/\<your user name\>/.m2
 * for Linux/Mac users, use find command to locate the .m2 directory

2. Implement the bidding strategy, acceptance strategy and opponent modelling by inherit from 
corresponding interfaces BiddingStrategy, AcceptanceStrategy and OpponentModel.
   * Note here: BiddingStrategy and AcceptanceStrategy interface exist in the BOA repository,
    you can access it by importing from geniusweb.boa.XXXX; OpponentModel exists in the Opponentmodel
     repository, access it by importing from geniusweb.opponentmodel.XXXX.
3. return the strategy objects in the methods getOpponentModel, getBiddingStrategy,
   getAccceptanceStrategy in the Group42Party class.
   

### How does the SOAP works on the object level?

1. First party's lastbid == null, so she will make an offer by send an Offer obejct which is a derived
class of Action.
2. The Action is sent via ConnectionEnd's send() method. ConnectionEnd is an interface which is inherited
by ProtocolToPartyConn interface.
3. The SAOPState class, of which is a field of SOAP object, contains a list of ProtocolToPartyConns for each of the parties.
4. The Action send in step 2 is wrapped in an ActionDone object and broadcast to all ProtocolToPartyConns by SOAP object.
5. Both parties at this time receive an ActionDone info. 
6. SOAP then will determine whether this is the final round. If not, it calls nextTurn and send a YourTurn info to 
the corresponding party.
7. The party who receives the YourTurn info, make her next action then. 
