# Docforia 0.1
A semistructured Multilayer Document Model

**This is an early release, the full release will be done in coming week. It is the text import/export feature that 
I am working that is not yet ready for release.**

## What?
This is a model that stores annotations in NLP (Natural Language Processing) applications. It is based on the idea 
of having layers of annotations such as Token, Sentence, Paragraph, etc. Being able to add properties, link annotations
and more.

The datastructure is similiar to a typed property graph with the added concept of ranges in a text.

The model provides an API for storage and query.

## Why?
We needed a way to store multiple layers of flattend information and be able to query and extract that information. We could not find one that matched our requirements so we built one.

## How?
[Full documentation](http://marcusklang.github.io/docforia/apidocs/)

Example usage
```java
 Document doc = new MemoryDocument("Greetings from Lund, Sweden!");
 //                                 01234567890123456789012345678

 Token Greetings   = new Token(doc).setRange(0,  9);
 Token from        = new Token(doc).setRange(10, 14);
 Token Lund        = new Token(doc).setRange(15, 19);
 Token comma       = new Token(doc).setRange(19, 20);
 Token Sweden      = new Token(doc).setRange(21, 27);
 Token exclamation = new Token(doc).setRange(27, 28);

 Sentence grettingsSentence = new Sentence(doc).setRange(0, 28);

 NamedEntity lundSwedenEntity
    = new NamedEntity(doc).setRange(Lund.getStart(), Sweden.getEnd())
                          .setLabel("Location");

 NodeTVar<Token> T = Token.var();
 NodeTVar<NamedEntity> NE = NamedEntity.var();

 List<Token> lundLocation = doc.select(T, NE)
                               .where(T).coveredBy(NE)
                               .orderByRange(T)
                               .query()
                               .map(GetNode.of(T))
                               .toList();

 assert lundLocation.size() == 3;
 for (Token token : lundLocation) {
    System.out.println(token);
 }
 // Outputs "Lund", ",", "Sweden"

 GroupProposition group = doc.select(T, NE)
                             .where(T).coveredBy(NE)
                             .orderByRange(T)
                             .groupBy(NE)
                             .query()
                             .first();

 NamedEntity ne = group.key(NE);
 System.out.println(ne);
 // Outputs "Lund, Sweden"

 assert group.list(T).size() == 3;
 for (Token token : group.list(T)) {
    System.out.println(token);
 }
 // Outputs "Lund", ",", "Sweden"
```
