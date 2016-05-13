# Docforia 1.0
A semistructured Multilayer Document Model with a query system and pluggable storage layer.

## What?
Docforia is data model and query system combined for use in NLP (Natural Language Processing) applications. It is based 
on the idea of having multiple layers of annotations such as Token, Sentence, Paragraph, etc. Being able to add 
properties, link annotations and more.

The underlying data model is similiar to a typed property graph with the added concept of ranges in a text.

## Why?
We needed a way to store multiple layers of flattend information and be able to query and extract that information. 
We could not find one that matched our requirements so we built one.

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
                              .stream()
                              .sorted(StreamUtils.orderBy(T))
                              .map(StreamUtils.toNode(T))
                              .collect(Collectors.toList());

assert lundLocation.size() == 3;
for (Token token : lundLocation) {
    System.out.println(token);
}

Optional<PropositionGroup> group = doc.select(T, NE)
                            .where(T).coveredBy(NE)
                            .stream()
                            .collect(QueryCollectors.groupBy(doc, NE).orderByValue(T).collector())
                            .stream()
                            .findFirst();

assertTrue(group.isPresent());

NamedEntity ne = group.get().key(NE);
System.out.println(ne);

assert group.get().list(T).size() == 3;
for (Token token : group.get().list(T)) {
    System.out.println(token);
}
```
