namespace java se.lth.cs.docforia.io.thrift
namespace py DocforiaThrift
namespace cpp docforia.thrift

struct TPropertySet {
    1: list<i32> keys;
}

enum TSpecialKey {
  NODE_RANGE_START_END = 1,
  NODE_NUM_EDGE_OUT = 2, //Type is D_INT (values are counts), layerid tells which layer
  EDGE_HEAD = 3, //Type is D_INT (values are node indicies), layerid tells which layer
  DOC_ID = 4,
  DOC_URI = 5,
  DOC_LANG = 6,
  DOC_TITLE = 7,
  DOC_TEXT = 8,
  DOC_TYPE = 9
}

enum TDataType {
  D_NULL = 0,
  D_BINARY = 1,
  D_STRING = 2,
  D_INT = 3,
  D_LONG = 4,
  D_FLOAT = 5,
  D_DOUBLE = 6,
  D_BOOLEAN = 7,
  D_STRING_ARRAY = 8,
  D_INT_ARRAY = 9,
  D_LONG_ARRAY = 10,
  D_FLOAT_ARRAY = 11,
  D_DOUBLE_ARRAY = 12,
  D_BOOLEAN_ARARY = 13,
  D_PROPERTY_MAP = 14,
  D_DOCUMENT = 15,
  D_DOCUMENT_ARRAY = 16
}

struct TPropertyKey {
  1: optional TSpecialKey special,
  2: optional string name
  3: optional TDataType type,
  4: optional i32 layerid
}

struct TPropertyValue {
  1: optional list<binary> binaryValue,
  2: optional list<string> stringValues,
  3: optional list<bool> boolValues,
  4: optional list<i32> intValues,
  5: optional list<i64> longValues,
  7: optional list<double> doubleValues,
  8: optional list<TDocument> docValues,
  9: optional list<TPropertyMap> propValues,
  10: optional list<i32> lengthInfo //In case of array values.
}

struct TPropertyMap {
  1: list<TPropertyKey> propkeys,
  2: list<TPropertyValue> propvalues
}

struct TColumn {
  1: list<binary> binaryValues;
  2: list<string> stringValues;
  3: list<bool> boolValues;
  4: list<i32> intValues;
  5: list<i64> longValues;
  7: list<double> doubleValues;
  8: list<TDocument> docValues;
  9: list<TPropertyMap> propmapValues;
  10: list<i32> lengthInfo; //In case of array values.
}

enum TNodeTypes {
  TOKEN = 0,
  SENTENCE = 1,
  PARAGRAPH = 2,

  ANCHOR = 3,

  SECTION = 4,
  ABSTRACT = 5,
  HEADING = 6,
  CLAUSE = 7,
  PHRASE = 8,

  PREDICATE = 9,
  ARGUMENT = 10,
  PROPOSITION = 11,

  ENTITY = 12,
  NAMED_ENTITY = 13,

  COREF_MENTION = 14,
  COREF_CHAIN = 15,

  MENTION = 16,
  ENTITY_DISAMBIGUATION = 17,
  NAMED_ENTITY_DISAMBIGUATION = 18,
  SENSE_DISAMBIGUATION = 19,
  COMPOUND = 20,

  LIST_ITEM = 21,
  LIST_SECTION = 22,

  TABLE_OF_CONTENTS = 23,
  AST_NODE = 24,
  AST_TEXT_NODE = 25,

  PARSE_TREE_NODE = 26
}


struct TNodes {
  1: optional TNodeTypes builtin;
  2: optional string name;
  3: list<string> variants;
  4: list<i32> numentries;
  5: list<TPropertySet> sets; // List of property sets, indicies corresponds to columns index.
  6: list<TPropertyKey> keys;
  7: list<TColumn> columns; // Raw data in sequence. Type is determined by keys.
}

enum TEdgeTypes {
  RELATIONSHIP = 0;
  DEPENDENCY_REL = 1;
  SEMANTIC_ROLE = 2;
  AST_EDGE = 3;
  PARSE_TREE_EDGE = 4;
}

struct TEdges {
  1: optional TEdgeTypes builtin;
  2: optional string name;
  3: list<string> variants;
  4: list<i32> numentries;
  5: list<TPropertySet> sets; // List of property sets, indicies corresponds to columns index.
  6: list<TPropertyKey> keys;
  7: list<TColumn> columns; // Raw data in sequence. Type is determined by keys.
}

struct TTypeStream {
    1: list<i32> stream; // List of set indicies, determines which set to use when decoding a single node.
}

struct TDocument {
  1: optional TPropertyMap propmap;
  2: list<TTypeStream> nodestreams;
  3: list<TTypeStream> edgestreams;
  4: list<TNodes> nodes;
  5: list<TEdges> edges;
}
