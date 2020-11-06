# Chandy-Misra-Haas-Algorithm
Chandy-Misra-Haas-Algorithm (OR Model) for  Deadlock Detection in Distributed Systems

* Keep both the StartProcess.java & icon.png in same location.
* The wait-for-graph is present between lines 18-23 as per WFG.png. (There are 6 processes considered)
  * The index, process name, port number pair follow 0,A,1000 - 1,B,2000 - 2,C,3000 - ... pattern
  * The main method requires process name & port number to be passed as parameters.
* Compile the StartProcess.java - javac StartProcess.java
* The program has to be run for each process separately. E.g. java StartProcess A 1000, java StartProcess B 2000 and so on
* Deadlock detection algorithm can be run from any process by click on the Initiate Deadlock Detection button on right.
* Each process window will show the log of all messages sent or received.
* The initiator process will log if deadlock is detected.
