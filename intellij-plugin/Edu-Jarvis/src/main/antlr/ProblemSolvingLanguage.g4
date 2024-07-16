grammar ProblemSolvingLanguage;

sentence: arbitraryText? expr arbitraryText? EOF;

arbitraryText : ( ARGUMENT | ARTICLE | MODIFIED | BY | IDENTIFIER | AND | VALUE | ELSE | THEN | LOOP | EACH | WHILE | DO | EMPTY | RANDOM | STRING_WORD | VARIABLE | SET | CALLED | FUNCTION | PRINT | STORE | CALL | IN | CREATE | BOOL | REPEAT | UNTIL | ADD | EQUAL | RETURN | READ | WITH | GET | CONST)+?;

word: STRING_WORD | (RANDOM STRING_WORD) | (EMPTY STRING_WORD) | NUMBER | CODE;

value: NUMBER | STRING | CODE | IDENTIFIER | BOOL;

contains: value arbitraryText? IN arbitraryText? CODE;

expr: PRINT arbitraryText? (STRING | CODE)?
    | CALL arbitraryText? FUNCTION? CODE (WITH (arbitraryText? value)+?)? arbitraryText? (GET arbitraryText? CODE)?
    | STORE arbitraryText? IN arbitraryText? VARIABLE? CODE
    | CREATE arbitraryText? word? VARIABLE? CALLED? CODE (EQUAL arbitraryText? word)?
    | SET arbitraryText? VALUE? arbitraryText? value
    | SET arbitraryText? VARIABLE? CALLED? (IDENTIFIER | CODE) arbitraryText? VALUE? value
    | IF arbitraryText? (BOOL | CODE | contains) arbitraryText? (EQUAL (NUMBER | STRING | CODE | IDENTIFIER))? (THEN? arbitraryText? expr arbitraryText?)? (ELSE arbitraryText? expr)?
    | REPEAT arbitraryText? (UNTIL | WHILE) arbitraryText?
    | WHILE arbitraryText? DO arbitraryText?
    | LOOP arbitraryText? EACH arbitraryText? CODE arbitraryText? IN arbitraryText? CODE (DO arbitraryText?)?
    | ADD arbitraryText? value arbitraryText? CODE
    | LOOP EACH CODE IN CODE
    | RETURN arbitraryText? value
    | READ arbitraryText?
    | GET arbitraryText?
    | SAVE arbitraryText? VARIABLE? CALLED? CODE
    | CODE FUNCTION? arbitraryText? RETURN arbitraryText? value
    | CODE EQUAL arbitraryText? word
    | expr AND (expr | arbitraryText)
    | expr arbitraryText? expr;


ARGUMENT: 'argument' | 'arguments';
ARTICLE: 'a' | 'an' | 'the';
MODIFIED: 'multiplied' | 'divided' | 'incremented' | 'decremented';
BY: 'by';
AND: 'and';
VALUE: 'value' | 'number';
IF: 'if';
ELSE: 'else' | 'otherwise';
THEN: 'then';
LOOP: 'loop' | 'for' | 'iterate';
EACH: 'each' | 'every' | 'all';
REPEAT: 'repeat' | 'continue';
UNTIL: 'until';
WHILE: 'while';
DO: 'do';
EMPTY: 'empty';
RANDOM: 'random';
STRING_WORD: 'string';
VARIABLE: VAL | VAR;
MUTABLE: 'mutable';
CONST: 'constant' | 'const';
VAR: 'var' | 'variable' | MUTABLE VAR;
VAL: 'val' | CONST VAL;
SET: 'set' | 'assign' | 'give' | 'initialize';
CALLED: 'called' | 'named';
FUNCTION: 'fun' | 'function';
PRINT: 'print' | 'prints' | 'output' | 'outputs' | 'display' | 'displays';
STORE: 'store' | 'stored' | 'stores';
CALL: 'call' | 'calls' | 'invoke' | 'invokes';
IN: 'in';
CREATE: 'declare' | 'declares' | 'set up' | 'sets up' | 'create' | 'creates';
BOOL: 'true' | 'false';
ADD: 'add' | 'adds' | 'append' | 'appends';
EQUAL: 'equal' | 'equals';
RETURN: 'return' | 'returns';
READ: 'read' | 'reads';
WITH: 'with';
GET: 'get' | 'gets' | 'receive' | 'receives' | 'obtain' | 'obtains';
SAVE: 'save';
NUMBER: [0-9]+;
STRING: '"'(~["\r\n])*'"';
CODE: '`'(~[`\r\n])*'`';
IDENTIFIER : [A-Za-z][A-Za-z0-9]*;
WS : [ \t\r\n,.:\\]+ -> skip;
