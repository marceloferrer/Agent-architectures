# Agent-architectures
MultiAgent Systems Agent architectures

Code developed with:
Aymen Merchaoui

The goal of this project is to develop an auction environment for a book trading simulation
using JADE, a Java-based multi-agent platform. The environment will consist of an
auctioneer agent and two types of bidder agents: reactive and deliberative.

To execute from eclipse use this line of arguments with jade.Boot as main class:
-gui -agents AuctionAgent:AgentArchitectures.AuctionAgent;Reactive1:AgentArchitectures.BidderAgent(0,50000,4);Delibera1:AgentArchitectures.BidderAgent(1,50000);Reactive2:AgentArchitectures.BidderAgent(0,100000,8);Delibera2:AgentArchitectures.BidderAgent(1,100000)
