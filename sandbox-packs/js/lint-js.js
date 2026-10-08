var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// node_modules/sucrase/dist/parser/tokenizer/keywords.js
var require_keywords = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/keywords.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var ContextualKeyword;
    (function(ContextualKeyword2) {
      const NONE = 0;
      ContextualKeyword2[ContextualKeyword2["NONE"] = NONE] = "NONE";
      const _abstract = NONE + 1;
      ContextualKeyword2[ContextualKeyword2["_abstract"] = _abstract] = "_abstract";
      const _accessor = _abstract + 1;
      ContextualKeyword2[ContextualKeyword2["_accessor"] = _accessor] = "_accessor";
      const _as = _accessor + 1;
      ContextualKeyword2[ContextualKeyword2["_as"] = _as] = "_as";
      const _assert = _as + 1;
      ContextualKeyword2[ContextualKeyword2["_assert"] = _assert] = "_assert";
      const _asserts = _assert + 1;
      ContextualKeyword2[ContextualKeyword2["_asserts"] = _asserts] = "_asserts";
      const _async = _asserts + 1;
      ContextualKeyword2[ContextualKeyword2["_async"] = _async] = "_async";
      const _await = _async + 1;
      ContextualKeyword2[ContextualKeyword2["_await"] = _await] = "_await";
      const _checks = _await + 1;
      ContextualKeyword2[ContextualKeyword2["_checks"] = _checks] = "_checks";
      const _constructor = _checks + 1;
      ContextualKeyword2[ContextualKeyword2["_constructor"] = _constructor] = "_constructor";
      const _declare = _constructor + 1;
      ContextualKeyword2[ContextualKeyword2["_declare"] = _declare] = "_declare";
      const _enum = _declare + 1;
      ContextualKeyword2[ContextualKeyword2["_enum"] = _enum] = "_enum";
      const _exports = _enum + 1;
      ContextualKeyword2[ContextualKeyword2["_exports"] = _exports] = "_exports";
      const _from = _exports + 1;
      ContextualKeyword2[ContextualKeyword2["_from"] = _from] = "_from";
      const _get = _from + 1;
      ContextualKeyword2[ContextualKeyword2["_get"] = _get] = "_get";
      const _global = _get + 1;
      ContextualKeyword2[ContextualKeyword2["_global"] = _global] = "_global";
      const _implements = _global + 1;
      ContextualKeyword2[ContextualKeyword2["_implements"] = _implements] = "_implements";
      const _infer = _implements + 1;
      ContextualKeyword2[ContextualKeyword2["_infer"] = _infer] = "_infer";
      const _interface = _infer + 1;
      ContextualKeyword2[ContextualKeyword2["_interface"] = _interface] = "_interface";
      const _is = _interface + 1;
      ContextualKeyword2[ContextualKeyword2["_is"] = _is] = "_is";
      const _keyof = _is + 1;
      ContextualKeyword2[ContextualKeyword2["_keyof"] = _keyof] = "_keyof";
      const _mixins = _keyof + 1;
      ContextualKeyword2[ContextualKeyword2["_mixins"] = _mixins] = "_mixins";
      const _module = _mixins + 1;
      ContextualKeyword2[ContextualKeyword2["_module"] = _module] = "_module";
      const _namespace = _module + 1;
      ContextualKeyword2[ContextualKeyword2["_namespace"] = _namespace] = "_namespace";
      const _of = _namespace + 1;
      ContextualKeyword2[ContextualKeyword2["_of"] = _of] = "_of";
      const _opaque = _of + 1;
      ContextualKeyword2[ContextualKeyword2["_opaque"] = _opaque] = "_opaque";
      const _out = _opaque + 1;
      ContextualKeyword2[ContextualKeyword2["_out"] = _out] = "_out";
      const _override = _out + 1;
      ContextualKeyword2[ContextualKeyword2["_override"] = _override] = "_override";
      const _private = _override + 1;
      ContextualKeyword2[ContextualKeyword2["_private"] = _private] = "_private";
      const _protected = _private + 1;
      ContextualKeyword2[ContextualKeyword2["_protected"] = _protected] = "_protected";
      const _proto = _protected + 1;
      ContextualKeyword2[ContextualKeyword2["_proto"] = _proto] = "_proto";
      const _public = _proto + 1;
      ContextualKeyword2[ContextualKeyword2["_public"] = _public] = "_public";
      const _readonly = _public + 1;
      ContextualKeyword2[ContextualKeyword2["_readonly"] = _readonly] = "_readonly";
      const _require = _readonly + 1;
      ContextualKeyword2[ContextualKeyword2["_require"] = _require] = "_require";
      const _satisfies = _require + 1;
      ContextualKeyword2[ContextualKeyword2["_satisfies"] = _satisfies] = "_satisfies";
      const _set = _satisfies + 1;
      ContextualKeyword2[ContextualKeyword2["_set"] = _set] = "_set";
      const _static = _set + 1;
      ContextualKeyword2[ContextualKeyword2["_static"] = _static] = "_static";
      const _symbol = _static + 1;
      ContextualKeyword2[ContextualKeyword2["_symbol"] = _symbol] = "_symbol";
      const _type = _symbol + 1;
      ContextualKeyword2[ContextualKeyword2["_type"] = _type] = "_type";
      const _unique = _type + 1;
      ContextualKeyword2[ContextualKeyword2["_unique"] = _unique] = "_unique";
      const _using = _unique + 1;
      ContextualKeyword2[ContextualKeyword2["_using"] = _using] = "_using";
    })(ContextualKeyword || (exports.ContextualKeyword = ContextualKeyword = {}));
  }
});

// node_modules/sucrase/dist/parser/tokenizer/types.js
var require_types = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/types.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var TokenType3;
    (function(TokenType4) {
      const PRECEDENCE_MASK = 15;
      TokenType4[TokenType4["PRECEDENCE_MASK"] = PRECEDENCE_MASK] = "PRECEDENCE_MASK";
      const IS_KEYWORD = 1 << 4;
      TokenType4[TokenType4["IS_KEYWORD"] = IS_KEYWORD] = "IS_KEYWORD";
      const IS_ASSIGN = 1 << 5;
      TokenType4[TokenType4["IS_ASSIGN"] = IS_ASSIGN] = "IS_ASSIGN";
      const IS_RIGHT_ASSOCIATIVE = 1 << 6;
      TokenType4[TokenType4["IS_RIGHT_ASSOCIATIVE"] = IS_RIGHT_ASSOCIATIVE] = "IS_RIGHT_ASSOCIATIVE";
      const IS_PREFIX = 1 << 7;
      TokenType4[TokenType4["IS_PREFIX"] = IS_PREFIX] = "IS_PREFIX";
      const IS_POSTFIX = 1 << 8;
      TokenType4[TokenType4["IS_POSTFIX"] = IS_POSTFIX] = "IS_POSTFIX";
      const IS_EXPRESSION_START = 1 << 9;
      TokenType4[TokenType4["IS_EXPRESSION_START"] = IS_EXPRESSION_START] = "IS_EXPRESSION_START";
      const num = 512;
      TokenType4[TokenType4["num"] = num] = "num";
      const bigint = 1536;
      TokenType4[TokenType4["bigint"] = bigint] = "bigint";
      const decimal = 2560;
      TokenType4[TokenType4["decimal"] = decimal] = "decimal";
      const regexp = 3584;
      TokenType4[TokenType4["regexp"] = regexp] = "regexp";
      const string = 4608;
      TokenType4[TokenType4["string"] = string] = "string";
      const name = 5632;
      TokenType4[TokenType4["name"] = name] = "name";
      const eof = 6144;
      TokenType4[TokenType4["eof"] = eof] = "eof";
      const bracketL = 7680;
      TokenType4[TokenType4["bracketL"] = bracketL] = "bracketL";
      const bracketR = 8192;
      TokenType4[TokenType4["bracketR"] = bracketR] = "bracketR";
      const braceL = 9728;
      TokenType4[TokenType4["braceL"] = braceL] = "braceL";
      const braceBarL = 10752;
      TokenType4[TokenType4["braceBarL"] = braceBarL] = "braceBarL";
      const braceR = 11264;
      TokenType4[TokenType4["braceR"] = braceR] = "braceR";
      const braceBarR = 12288;
      TokenType4[TokenType4["braceBarR"] = braceBarR] = "braceBarR";
      const parenL = 13824;
      TokenType4[TokenType4["parenL"] = parenL] = "parenL";
      const parenR = 14336;
      TokenType4[TokenType4["parenR"] = parenR] = "parenR";
      const comma = 15360;
      TokenType4[TokenType4["comma"] = comma] = "comma";
      const semi = 16384;
      TokenType4[TokenType4["semi"] = semi] = "semi";
      const colon = 17408;
      TokenType4[TokenType4["colon"] = colon] = "colon";
      const doubleColon = 18432;
      TokenType4[TokenType4["doubleColon"] = doubleColon] = "doubleColon";
      const dot = 19456;
      TokenType4[TokenType4["dot"] = dot] = "dot";
      const question = 20480;
      TokenType4[TokenType4["question"] = question] = "question";
      const questionDot = 21504;
      TokenType4[TokenType4["questionDot"] = questionDot] = "questionDot";
      const arrow = 22528;
      TokenType4[TokenType4["arrow"] = arrow] = "arrow";
      const template = 23552;
      TokenType4[TokenType4["template"] = template] = "template";
      const ellipsis = 24576;
      TokenType4[TokenType4["ellipsis"] = ellipsis] = "ellipsis";
      const backQuote = 25600;
      TokenType4[TokenType4["backQuote"] = backQuote] = "backQuote";
      const dollarBraceL = 27136;
      TokenType4[TokenType4["dollarBraceL"] = dollarBraceL] = "dollarBraceL";
      const at2 = 27648;
      TokenType4[TokenType4["at"] = at2] = "at";
      const hash = 29184;
      TokenType4[TokenType4["hash"] = hash] = "hash";
      const eq = 29728;
      TokenType4[TokenType4["eq"] = eq] = "eq";
      const assign = 30752;
      TokenType4[TokenType4["assign"] = assign] = "assign";
      const preIncDec = 32640;
      TokenType4[TokenType4["preIncDec"] = preIncDec] = "preIncDec";
      const postIncDec = 33664;
      TokenType4[TokenType4["postIncDec"] = postIncDec] = "postIncDec";
      const bang = 34432;
      TokenType4[TokenType4["bang"] = bang] = "bang";
      const tilde = 35456;
      TokenType4[TokenType4["tilde"] = tilde] = "tilde";
      const pipeline = 35841;
      TokenType4[TokenType4["pipeline"] = pipeline] = "pipeline";
      const nullishCoalescing = 36866;
      TokenType4[TokenType4["nullishCoalescing"] = nullishCoalescing] = "nullishCoalescing";
      const logicalOR = 37890;
      TokenType4[TokenType4["logicalOR"] = logicalOR] = "logicalOR";
      const logicalAND = 38915;
      TokenType4[TokenType4["logicalAND"] = logicalAND] = "logicalAND";
      const bitwiseOR = 39940;
      TokenType4[TokenType4["bitwiseOR"] = bitwiseOR] = "bitwiseOR";
      const bitwiseXOR = 40965;
      TokenType4[TokenType4["bitwiseXOR"] = bitwiseXOR] = "bitwiseXOR";
      const bitwiseAND = 41990;
      TokenType4[TokenType4["bitwiseAND"] = bitwiseAND] = "bitwiseAND";
      const equality = 43015;
      TokenType4[TokenType4["equality"] = equality] = "equality";
      const lessThan = 44040;
      TokenType4[TokenType4["lessThan"] = lessThan] = "lessThan";
      const greaterThan = 45064;
      TokenType4[TokenType4["greaterThan"] = greaterThan] = "greaterThan";
      const relationalOrEqual = 46088;
      TokenType4[TokenType4["relationalOrEqual"] = relationalOrEqual] = "relationalOrEqual";
      const bitShiftL = 47113;
      TokenType4[TokenType4["bitShiftL"] = bitShiftL] = "bitShiftL";
      const bitShiftR = 48137;
      TokenType4[TokenType4["bitShiftR"] = bitShiftR] = "bitShiftR";
      const plus = 49802;
      TokenType4[TokenType4["plus"] = plus] = "plus";
      const minus = 50826;
      TokenType4[TokenType4["minus"] = minus] = "minus";
      const modulo = 51723;
      TokenType4[TokenType4["modulo"] = modulo] = "modulo";
      const star = 52235;
      TokenType4[TokenType4["star"] = star] = "star";
      const slash = 53259;
      TokenType4[TokenType4["slash"] = slash] = "slash";
      const exponent = 54348;
      TokenType4[TokenType4["exponent"] = exponent] = "exponent";
      const jsxName = 55296;
      TokenType4[TokenType4["jsxName"] = jsxName] = "jsxName";
      const jsxText = 56320;
      TokenType4[TokenType4["jsxText"] = jsxText] = "jsxText";
      const jsxEmptyText = 57344;
      TokenType4[TokenType4["jsxEmptyText"] = jsxEmptyText] = "jsxEmptyText";
      const jsxTagStart = 58880;
      TokenType4[TokenType4["jsxTagStart"] = jsxTagStart] = "jsxTagStart";
      const jsxTagEnd = 59392;
      TokenType4[TokenType4["jsxTagEnd"] = jsxTagEnd] = "jsxTagEnd";
      const typeParameterStart = 60928;
      TokenType4[TokenType4["typeParameterStart"] = typeParameterStart] = "typeParameterStart";
      const nonNullAssertion = 61440;
      TokenType4[TokenType4["nonNullAssertion"] = nonNullAssertion] = "nonNullAssertion";
      const _break = 62480;
      TokenType4[TokenType4["_break"] = _break] = "_break";
      const _case = 63504;
      TokenType4[TokenType4["_case"] = _case] = "_case";
      const _catch = 64528;
      TokenType4[TokenType4["_catch"] = _catch] = "_catch";
      const _continue = 65552;
      TokenType4[TokenType4["_continue"] = _continue] = "_continue";
      const _debugger = 66576;
      TokenType4[TokenType4["_debugger"] = _debugger] = "_debugger";
      const _default = 67600;
      TokenType4[TokenType4["_default"] = _default] = "_default";
      const _do = 68624;
      TokenType4[TokenType4["_do"] = _do] = "_do";
      const _else = 69648;
      TokenType4[TokenType4["_else"] = _else] = "_else";
      const _finally = 70672;
      TokenType4[TokenType4["_finally"] = _finally] = "_finally";
      const _for = 71696;
      TokenType4[TokenType4["_for"] = _for] = "_for";
      const _function = 73232;
      TokenType4[TokenType4["_function"] = _function] = "_function";
      const _if = 73744;
      TokenType4[TokenType4["_if"] = _if] = "_if";
      const _return = 74768;
      TokenType4[TokenType4["_return"] = _return] = "_return";
      const _switch = 75792;
      TokenType4[TokenType4["_switch"] = _switch] = "_switch";
      const _throw = 77456;
      TokenType4[TokenType4["_throw"] = _throw] = "_throw";
      const _try = 77840;
      TokenType4[TokenType4["_try"] = _try] = "_try";
      const _var = 78864;
      TokenType4[TokenType4["_var"] = _var] = "_var";
      const _let = 79888;
      TokenType4[TokenType4["_let"] = _let] = "_let";
      const _const = 80912;
      TokenType4[TokenType4["_const"] = _const] = "_const";
      const _while = 81936;
      TokenType4[TokenType4["_while"] = _while] = "_while";
      const _with = 82960;
      TokenType4[TokenType4["_with"] = _with] = "_with";
      const _new = 84496;
      TokenType4[TokenType4["_new"] = _new] = "_new";
      const _this = 85520;
      TokenType4[TokenType4["_this"] = _this] = "_this";
      const _super = 86544;
      TokenType4[TokenType4["_super"] = _super] = "_super";
      const _class = 87568;
      TokenType4[TokenType4["_class"] = _class] = "_class";
      const _extends = 88080;
      TokenType4[TokenType4["_extends"] = _extends] = "_extends";
      const _export = 89104;
      TokenType4[TokenType4["_export"] = _export] = "_export";
      const _import = 90640;
      TokenType4[TokenType4["_import"] = _import] = "_import";
      const _yield = 91664;
      TokenType4[TokenType4["_yield"] = _yield] = "_yield";
      const _null = 92688;
      TokenType4[TokenType4["_null"] = _null] = "_null";
      const _true = 93712;
      TokenType4[TokenType4["_true"] = _true] = "_true";
      const _false = 94736;
      TokenType4[TokenType4["_false"] = _false] = "_false";
      const _in = 95256;
      TokenType4[TokenType4["_in"] = _in] = "_in";
      const _instanceof = 96280;
      TokenType4[TokenType4["_instanceof"] = _instanceof] = "_instanceof";
      const _typeof = 97936;
      TokenType4[TokenType4["_typeof"] = _typeof] = "_typeof";
      const _void = 98960;
      TokenType4[TokenType4["_void"] = _void] = "_void";
      const _delete = 99984;
      TokenType4[TokenType4["_delete"] = _delete] = "_delete";
      const _async = 100880;
      TokenType4[TokenType4["_async"] = _async] = "_async";
      const _get = 101904;
      TokenType4[TokenType4["_get"] = _get] = "_get";
      const _set = 102928;
      TokenType4[TokenType4["_set"] = _set] = "_set";
      const _declare = 103952;
      TokenType4[TokenType4["_declare"] = _declare] = "_declare";
      const _readonly = 104976;
      TokenType4[TokenType4["_readonly"] = _readonly] = "_readonly";
      const _abstract = 106e3;
      TokenType4[TokenType4["_abstract"] = _abstract] = "_abstract";
      const _static = 107024;
      TokenType4[TokenType4["_static"] = _static] = "_static";
      const _public = 107536;
      TokenType4[TokenType4["_public"] = _public] = "_public";
      const _private = 108560;
      TokenType4[TokenType4["_private"] = _private] = "_private";
      const _protected = 109584;
      TokenType4[TokenType4["_protected"] = _protected] = "_protected";
      const _override = 110608;
      TokenType4[TokenType4["_override"] = _override] = "_override";
      const _as = 112144;
      TokenType4[TokenType4["_as"] = _as] = "_as";
      const _enum = 113168;
      TokenType4[TokenType4["_enum"] = _enum] = "_enum";
      const _type = 114192;
      TokenType4[TokenType4["_type"] = _type] = "_type";
      const _implements = 115216;
      TokenType4[TokenType4["_implements"] = _implements] = "_implements";
    })(TokenType3 || (exports.TokenType = TokenType3 = {}));
    function formatTokenType(tokenType) {
      switch (tokenType) {
        case TokenType3.num:
          return "num";
        case TokenType3.bigint:
          return "bigint";
        case TokenType3.decimal:
          return "decimal";
        case TokenType3.regexp:
          return "regexp";
        case TokenType3.string:
          return "string";
        case TokenType3.name:
          return "name";
        case TokenType3.eof:
          return "eof";
        case TokenType3.bracketL:
          return "[";
        case TokenType3.bracketR:
          return "]";
        case TokenType3.braceL:
          return "{";
        case TokenType3.braceBarL:
          return "{|";
        case TokenType3.braceR:
          return "}";
        case TokenType3.braceBarR:
          return "|}";
        case TokenType3.parenL:
          return "(";
        case TokenType3.parenR:
          return ")";
        case TokenType3.comma:
          return ",";
        case TokenType3.semi:
          return ";";
        case TokenType3.colon:
          return ":";
        case TokenType3.doubleColon:
          return "::";
        case TokenType3.dot:
          return ".";
        case TokenType3.question:
          return "?";
        case TokenType3.questionDot:
          return "?.";
        case TokenType3.arrow:
          return "=>";
        case TokenType3.template:
          return "template";
        case TokenType3.ellipsis:
          return "...";
        case TokenType3.backQuote:
          return "`";
        case TokenType3.dollarBraceL:
          return "${";
        case TokenType3.at:
          return "@";
        case TokenType3.hash:
          return "#";
        case TokenType3.eq:
          return "=";
        case TokenType3.assign:
          return "_=";
        case TokenType3.preIncDec:
          return "++/--";
        case TokenType3.postIncDec:
          return "++/--";
        case TokenType3.bang:
          return "!";
        case TokenType3.tilde:
          return "~";
        case TokenType3.pipeline:
          return "|>";
        case TokenType3.nullishCoalescing:
          return "??";
        case TokenType3.logicalOR:
          return "||";
        case TokenType3.logicalAND:
          return "&&";
        case TokenType3.bitwiseOR:
          return "|";
        case TokenType3.bitwiseXOR:
          return "^";
        case TokenType3.bitwiseAND:
          return "&";
        case TokenType3.equality:
          return "==/!=";
        case TokenType3.lessThan:
          return "<";
        case TokenType3.greaterThan:
          return ">";
        case TokenType3.relationalOrEqual:
          return "<=/>=";
        case TokenType3.bitShiftL:
          return "<<";
        case TokenType3.bitShiftR:
          return ">>/>>>";
        case TokenType3.plus:
          return "+";
        case TokenType3.minus:
          return "-";
        case TokenType3.modulo:
          return "%";
        case TokenType3.star:
          return "*";
        case TokenType3.slash:
          return "/";
        case TokenType3.exponent:
          return "**";
        case TokenType3.jsxName:
          return "jsxName";
        case TokenType3.jsxText:
          return "jsxText";
        case TokenType3.jsxEmptyText:
          return "jsxEmptyText";
        case TokenType3.jsxTagStart:
          return "jsxTagStart";
        case TokenType3.jsxTagEnd:
          return "jsxTagEnd";
        case TokenType3.typeParameterStart:
          return "typeParameterStart";
        case TokenType3.nonNullAssertion:
          return "nonNullAssertion";
        case TokenType3._break:
          return "break";
        case TokenType3._case:
          return "case";
        case TokenType3._catch:
          return "catch";
        case TokenType3._continue:
          return "continue";
        case TokenType3._debugger:
          return "debugger";
        case TokenType3._default:
          return "default";
        case TokenType3._do:
          return "do";
        case TokenType3._else:
          return "else";
        case TokenType3._finally:
          return "finally";
        case TokenType3._for:
          return "for";
        case TokenType3._function:
          return "function";
        case TokenType3._if:
          return "if";
        case TokenType3._return:
          return "return";
        case TokenType3._switch:
          return "switch";
        case TokenType3._throw:
          return "throw";
        case TokenType3._try:
          return "try";
        case TokenType3._var:
          return "var";
        case TokenType3._let:
          return "let";
        case TokenType3._const:
          return "const";
        case TokenType3._while:
          return "while";
        case TokenType3._with:
          return "with";
        case TokenType3._new:
          return "new";
        case TokenType3._this:
          return "this";
        case TokenType3._super:
          return "super";
        case TokenType3._class:
          return "class";
        case TokenType3._extends:
          return "extends";
        case TokenType3._export:
          return "export";
        case TokenType3._import:
          return "import";
        case TokenType3._yield:
          return "yield";
        case TokenType3._null:
          return "null";
        case TokenType3._true:
          return "true";
        case TokenType3._false:
          return "false";
        case TokenType3._in:
          return "in";
        case TokenType3._instanceof:
          return "instanceof";
        case TokenType3._typeof:
          return "typeof";
        case TokenType3._void:
          return "void";
        case TokenType3._delete:
          return "delete";
        case TokenType3._async:
          return "async";
        case TokenType3._get:
          return "get";
        case TokenType3._set:
          return "set";
        case TokenType3._declare:
          return "declare";
        case TokenType3._readonly:
          return "readonly";
        case TokenType3._abstract:
          return "abstract";
        case TokenType3._static:
          return "static";
        case TokenType3._public:
          return "public";
        case TokenType3._private:
          return "private";
        case TokenType3._protected:
          return "protected";
        case TokenType3._override:
          return "override";
        case TokenType3._as:
          return "as";
        case TokenType3._enum:
          return "enum";
        case TokenType3._type:
          return "type";
        case TokenType3._implements:
          return "implements";
        default:
          return "";
      }
    }
    exports.formatTokenType = formatTokenType;
  }
});

// node_modules/sucrase/dist/parser/tokenizer/state.js
var require_state = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/state.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    var _types = require_types();
    var Scope3 = class {
      constructor(startTokenIndex, endTokenIndex, isFunctionScope) {
        this.startTokenIndex = startTokenIndex;
        this.endTokenIndex = endTokenIndex;
        this.isFunctionScope = isFunctionScope;
      }
    };
    exports.Scope = Scope3;
    var StateSnapshot = class {
      constructor(potentialArrowAt, noAnonFunctionType, inDisallowConditionalTypesContext, tokensLength, scopesLength, pos, type, contextualKeyword, start, end, isType, scopeDepth, error) {
        ;
        this.potentialArrowAt = potentialArrowAt;
        this.noAnonFunctionType = noAnonFunctionType;
        this.inDisallowConditionalTypesContext = inDisallowConditionalTypesContext;
        this.tokensLength = tokensLength;
        this.scopesLength = scopesLength;
        this.pos = pos;
        this.type = type;
        this.contextualKeyword = contextualKeyword;
        this.start = start;
        this.end = end;
        this.isType = isType;
        this.scopeDepth = scopeDepth;
        this.error = error;
      }
    };
    exports.StateSnapshot = StateSnapshot;
    var State = class _State {
      constructor() {
        _State.prototype.__init.call(this);
        _State.prototype.__init2.call(this);
        _State.prototype.__init3.call(this);
        _State.prototype.__init4.call(this);
        _State.prototype.__init5.call(this);
        _State.prototype.__init6.call(this);
        _State.prototype.__init7.call(this);
        _State.prototype.__init8.call(this);
        _State.prototype.__init9.call(this);
        _State.prototype.__init10.call(this);
        _State.prototype.__init11.call(this);
        _State.prototype.__init12.call(this);
        _State.prototype.__init13.call(this);
      }
      // Used to signify the start of a potential arrow function
      __init() {
        this.potentialArrowAt = -1;
      }
      // Used by Flow to handle an edge case involving function type parsing.
      __init2() {
        this.noAnonFunctionType = false;
      }
      // Used by TypeScript to handle ambiguities when parsing conditional types.
      __init3() {
        this.inDisallowConditionalTypesContext = false;
      }
      // Token store.
      __init4() {
        this.tokens = [];
      }
      // Array of all observed scopes, ordered by their ending position.
      __init5() {
        this.scopes = [];
      }
      // The current position of the tokenizer in the input.
      __init6() {
        this.pos = 0;
      }
      // Information about the current token.
      __init7() {
        this.type = _types.TokenType.eof;
      }
      __init8() {
        this.contextualKeyword = _keywords.ContextualKeyword.NONE;
      }
      __init9() {
        this.start = 0;
      }
      __init10() {
        this.end = 0;
      }
      __init11() {
        this.isType = false;
      }
      __init12() {
        this.scopeDepth = 0;
      }
      /**
       * If the parser is in an error state, then the token is always tt.eof and all functions can
       * keep executing but should be written so they don't get into an infinite loop in this situation.
       *
       * This approach, combined with the ability to snapshot and restore state, allows us to implement
       * backtracking without exceptions and without needing to explicitly propagate error states
       * everywhere.
       */
      __init13() {
        this.error = null;
      }
      snapshot() {
        return new StateSnapshot(
          this.potentialArrowAt,
          this.noAnonFunctionType,
          this.inDisallowConditionalTypesContext,
          this.tokens.length,
          this.scopes.length,
          this.pos,
          this.type,
          this.contextualKeyword,
          this.start,
          this.end,
          this.isType,
          this.scopeDepth,
          this.error
        );
      }
      restoreFromSnapshot(snapshot) {
        this.potentialArrowAt = snapshot.potentialArrowAt;
        this.noAnonFunctionType = snapshot.noAnonFunctionType;
        this.inDisallowConditionalTypesContext = snapshot.inDisallowConditionalTypesContext;
        this.tokens.length = snapshot.tokensLength;
        this.scopes.length = snapshot.scopesLength;
        this.pos = snapshot.pos;
        this.type = snapshot.type;
        this.contextualKeyword = snapshot.contextualKeyword;
        this.start = snapshot.start;
        this.end = snapshot.end;
        this.isType = snapshot.isType;
        this.scopeDepth = snapshot.scopeDepth;
        this.error = snapshot.error;
      }
    };
    exports.default = State;
  }
});

// node_modules/sucrase/dist/parser/util/charcodes.js
var require_charcodes = __commonJS({
  "node_modules/sucrase/dist/parser/util/charcodes.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var charCodes;
    (function(charCodes2) {
      const backSpace = 8;
      charCodes2[charCodes2["backSpace"] = backSpace] = "backSpace";
      const lineFeed = 10;
      charCodes2[charCodes2["lineFeed"] = lineFeed] = "lineFeed";
      const tab = 9;
      charCodes2[charCodes2["tab"] = tab] = "tab";
      const carriageReturn = 13;
      charCodes2[charCodes2["carriageReturn"] = carriageReturn] = "carriageReturn";
      const shiftOut = 14;
      charCodes2[charCodes2["shiftOut"] = shiftOut] = "shiftOut";
      const space = 32;
      charCodes2[charCodes2["space"] = space] = "space";
      const exclamationMark = 33;
      charCodes2[charCodes2["exclamationMark"] = exclamationMark] = "exclamationMark";
      const quotationMark = 34;
      charCodes2[charCodes2["quotationMark"] = quotationMark] = "quotationMark";
      const numberSign = 35;
      charCodes2[charCodes2["numberSign"] = numberSign] = "numberSign";
      const dollarSign = 36;
      charCodes2[charCodes2["dollarSign"] = dollarSign] = "dollarSign";
      const percentSign = 37;
      charCodes2[charCodes2["percentSign"] = percentSign] = "percentSign";
      const ampersand = 38;
      charCodes2[charCodes2["ampersand"] = ampersand] = "ampersand";
      const apostrophe = 39;
      charCodes2[charCodes2["apostrophe"] = apostrophe] = "apostrophe";
      const leftParenthesis = 40;
      charCodes2[charCodes2["leftParenthesis"] = leftParenthesis] = "leftParenthesis";
      const rightParenthesis = 41;
      charCodes2[charCodes2["rightParenthesis"] = rightParenthesis] = "rightParenthesis";
      const asterisk = 42;
      charCodes2[charCodes2["asterisk"] = asterisk] = "asterisk";
      const plusSign = 43;
      charCodes2[charCodes2["plusSign"] = plusSign] = "plusSign";
      const comma = 44;
      charCodes2[charCodes2["comma"] = comma] = "comma";
      const dash = 45;
      charCodes2[charCodes2["dash"] = dash] = "dash";
      const dot = 46;
      charCodes2[charCodes2["dot"] = dot] = "dot";
      const slash = 47;
      charCodes2[charCodes2["slash"] = slash] = "slash";
      const digit0 = 48;
      charCodes2[charCodes2["digit0"] = digit0] = "digit0";
      const digit1 = 49;
      charCodes2[charCodes2["digit1"] = digit1] = "digit1";
      const digit2 = 50;
      charCodes2[charCodes2["digit2"] = digit2] = "digit2";
      const digit3 = 51;
      charCodes2[charCodes2["digit3"] = digit3] = "digit3";
      const digit4 = 52;
      charCodes2[charCodes2["digit4"] = digit4] = "digit4";
      const digit5 = 53;
      charCodes2[charCodes2["digit5"] = digit5] = "digit5";
      const digit6 = 54;
      charCodes2[charCodes2["digit6"] = digit6] = "digit6";
      const digit7 = 55;
      charCodes2[charCodes2["digit7"] = digit7] = "digit7";
      const digit8 = 56;
      charCodes2[charCodes2["digit8"] = digit8] = "digit8";
      const digit9 = 57;
      charCodes2[charCodes2["digit9"] = digit9] = "digit9";
      const colon = 58;
      charCodes2[charCodes2["colon"] = colon] = "colon";
      const semicolon = 59;
      charCodes2[charCodes2["semicolon"] = semicolon] = "semicolon";
      const lessThan = 60;
      charCodes2[charCodes2["lessThan"] = lessThan] = "lessThan";
      const equalsTo = 61;
      charCodes2[charCodes2["equalsTo"] = equalsTo] = "equalsTo";
      const greaterThan = 62;
      charCodes2[charCodes2["greaterThan"] = greaterThan] = "greaterThan";
      const questionMark = 63;
      charCodes2[charCodes2["questionMark"] = questionMark] = "questionMark";
      const atSign = 64;
      charCodes2[charCodes2["atSign"] = atSign] = "atSign";
      const uppercaseA = 65;
      charCodes2[charCodes2["uppercaseA"] = uppercaseA] = "uppercaseA";
      const uppercaseB = 66;
      charCodes2[charCodes2["uppercaseB"] = uppercaseB] = "uppercaseB";
      const uppercaseC = 67;
      charCodes2[charCodes2["uppercaseC"] = uppercaseC] = "uppercaseC";
      const uppercaseD = 68;
      charCodes2[charCodes2["uppercaseD"] = uppercaseD] = "uppercaseD";
      const uppercaseE = 69;
      charCodes2[charCodes2["uppercaseE"] = uppercaseE] = "uppercaseE";
      const uppercaseF = 70;
      charCodes2[charCodes2["uppercaseF"] = uppercaseF] = "uppercaseF";
      const uppercaseG = 71;
      charCodes2[charCodes2["uppercaseG"] = uppercaseG] = "uppercaseG";
      const uppercaseH = 72;
      charCodes2[charCodes2["uppercaseH"] = uppercaseH] = "uppercaseH";
      const uppercaseI = 73;
      charCodes2[charCodes2["uppercaseI"] = uppercaseI] = "uppercaseI";
      const uppercaseJ = 74;
      charCodes2[charCodes2["uppercaseJ"] = uppercaseJ] = "uppercaseJ";
      const uppercaseK = 75;
      charCodes2[charCodes2["uppercaseK"] = uppercaseK] = "uppercaseK";
      const uppercaseL = 76;
      charCodes2[charCodes2["uppercaseL"] = uppercaseL] = "uppercaseL";
      const uppercaseM = 77;
      charCodes2[charCodes2["uppercaseM"] = uppercaseM] = "uppercaseM";
      const uppercaseN = 78;
      charCodes2[charCodes2["uppercaseN"] = uppercaseN] = "uppercaseN";
      const uppercaseO = 79;
      charCodes2[charCodes2["uppercaseO"] = uppercaseO] = "uppercaseO";
      const uppercaseP = 80;
      charCodes2[charCodes2["uppercaseP"] = uppercaseP] = "uppercaseP";
      const uppercaseQ = 81;
      charCodes2[charCodes2["uppercaseQ"] = uppercaseQ] = "uppercaseQ";
      const uppercaseR = 82;
      charCodes2[charCodes2["uppercaseR"] = uppercaseR] = "uppercaseR";
      const uppercaseS = 83;
      charCodes2[charCodes2["uppercaseS"] = uppercaseS] = "uppercaseS";
      const uppercaseT = 84;
      charCodes2[charCodes2["uppercaseT"] = uppercaseT] = "uppercaseT";
      const uppercaseU = 85;
      charCodes2[charCodes2["uppercaseU"] = uppercaseU] = "uppercaseU";
      const uppercaseV = 86;
      charCodes2[charCodes2["uppercaseV"] = uppercaseV] = "uppercaseV";
      const uppercaseW = 87;
      charCodes2[charCodes2["uppercaseW"] = uppercaseW] = "uppercaseW";
      const uppercaseX = 88;
      charCodes2[charCodes2["uppercaseX"] = uppercaseX] = "uppercaseX";
      const uppercaseY = 89;
      charCodes2[charCodes2["uppercaseY"] = uppercaseY] = "uppercaseY";
      const uppercaseZ = 90;
      charCodes2[charCodes2["uppercaseZ"] = uppercaseZ] = "uppercaseZ";
      const leftSquareBracket = 91;
      charCodes2[charCodes2["leftSquareBracket"] = leftSquareBracket] = "leftSquareBracket";
      const backslash = 92;
      charCodes2[charCodes2["backslash"] = backslash] = "backslash";
      const rightSquareBracket = 93;
      charCodes2[charCodes2["rightSquareBracket"] = rightSquareBracket] = "rightSquareBracket";
      const caret = 94;
      charCodes2[charCodes2["caret"] = caret] = "caret";
      const underscore = 95;
      charCodes2[charCodes2["underscore"] = underscore] = "underscore";
      const graveAccent = 96;
      charCodes2[charCodes2["graveAccent"] = graveAccent] = "graveAccent";
      const lowercaseA = 97;
      charCodes2[charCodes2["lowercaseA"] = lowercaseA] = "lowercaseA";
      const lowercaseB = 98;
      charCodes2[charCodes2["lowercaseB"] = lowercaseB] = "lowercaseB";
      const lowercaseC = 99;
      charCodes2[charCodes2["lowercaseC"] = lowercaseC] = "lowercaseC";
      const lowercaseD = 100;
      charCodes2[charCodes2["lowercaseD"] = lowercaseD] = "lowercaseD";
      const lowercaseE = 101;
      charCodes2[charCodes2["lowercaseE"] = lowercaseE] = "lowercaseE";
      const lowercaseF = 102;
      charCodes2[charCodes2["lowercaseF"] = lowercaseF] = "lowercaseF";
      const lowercaseG = 103;
      charCodes2[charCodes2["lowercaseG"] = lowercaseG] = "lowercaseG";
      const lowercaseH = 104;
      charCodes2[charCodes2["lowercaseH"] = lowercaseH] = "lowercaseH";
      const lowercaseI = 105;
      charCodes2[charCodes2["lowercaseI"] = lowercaseI] = "lowercaseI";
      const lowercaseJ = 106;
      charCodes2[charCodes2["lowercaseJ"] = lowercaseJ] = "lowercaseJ";
      const lowercaseK = 107;
      charCodes2[charCodes2["lowercaseK"] = lowercaseK] = "lowercaseK";
      const lowercaseL = 108;
      charCodes2[charCodes2["lowercaseL"] = lowercaseL] = "lowercaseL";
      const lowercaseM = 109;
      charCodes2[charCodes2["lowercaseM"] = lowercaseM] = "lowercaseM";
      const lowercaseN = 110;
      charCodes2[charCodes2["lowercaseN"] = lowercaseN] = "lowercaseN";
      const lowercaseO = 111;
      charCodes2[charCodes2["lowercaseO"] = lowercaseO] = "lowercaseO";
      const lowercaseP = 112;
      charCodes2[charCodes2["lowercaseP"] = lowercaseP] = "lowercaseP";
      const lowercaseQ = 113;
      charCodes2[charCodes2["lowercaseQ"] = lowercaseQ] = "lowercaseQ";
      const lowercaseR = 114;
      charCodes2[charCodes2["lowercaseR"] = lowercaseR] = "lowercaseR";
      const lowercaseS = 115;
      charCodes2[charCodes2["lowercaseS"] = lowercaseS] = "lowercaseS";
      const lowercaseT = 116;
      charCodes2[charCodes2["lowercaseT"] = lowercaseT] = "lowercaseT";
      const lowercaseU = 117;
      charCodes2[charCodes2["lowercaseU"] = lowercaseU] = "lowercaseU";
      const lowercaseV = 118;
      charCodes2[charCodes2["lowercaseV"] = lowercaseV] = "lowercaseV";
      const lowercaseW = 119;
      charCodes2[charCodes2["lowercaseW"] = lowercaseW] = "lowercaseW";
      const lowercaseX = 120;
      charCodes2[charCodes2["lowercaseX"] = lowercaseX] = "lowercaseX";
      const lowercaseY = 121;
      charCodes2[charCodes2["lowercaseY"] = lowercaseY] = "lowercaseY";
      const lowercaseZ = 122;
      charCodes2[charCodes2["lowercaseZ"] = lowercaseZ] = "lowercaseZ";
      const leftCurlyBrace = 123;
      charCodes2[charCodes2["leftCurlyBrace"] = leftCurlyBrace] = "leftCurlyBrace";
      const verticalBar = 124;
      charCodes2[charCodes2["verticalBar"] = verticalBar] = "verticalBar";
      const rightCurlyBrace = 125;
      charCodes2[charCodes2["rightCurlyBrace"] = rightCurlyBrace] = "rightCurlyBrace";
      const tilde = 126;
      charCodes2[charCodes2["tilde"] = tilde] = "tilde";
      const nonBreakingSpace = 160;
      charCodes2[charCodes2["nonBreakingSpace"] = nonBreakingSpace] = "nonBreakingSpace";
      const oghamSpaceMark = 5760;
      charCodes2[charCodes2["oghamSpaceMark"] = oghamSpaceMark] = "oghamSpaceMark";
      const lineSeparator = 8232;
      charCodes2[charCodes2["lineSeparator"] = lineSeparator] = "lineSeparator";
      const paragraphSeparator = 8233;
      charCodes2[charCodes2["paragraphSeparator"] = paragraphSeparator] = "paragraphSeparator";
    })(charCodes || (exports.charCodes = charCodes = {}));
    function isDigit(code) {
      return code >= charCodes.digit0 && code <= charCodes.digit9 || code >= charCodes.lowercaseA && code <= charCodes.lowercaseF || code >= charCodes.uppercaseA && code <= charCodes.uppercaseF;
    }
    exports.isDigit = isDigit;
  }
});

// node_modules/sucrase/dist/parser/traverser/base.js
var require_base = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/base.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _state = require_state();
    var _state2 = _interopRequireDefault(_state);
    var _charcodes = require_charcodes();
    exports.isJSXEnabled;
    exports.isTypeScriptEnabled;
    exports.isFlowEnabled;
    exports.state;
    exports.input;
    exports.nextContextId;
    function getNextContextId() {
      return exports.nextContextId++;
    }
    exports.getNextContextId = getNextContextId;
    function augmentError(error) {
      if ("pos" in error) {
        const loc = locationForIndex(error.pos);
        error.message += ` (${loc.line}:${loc.column})`;
        error.loc = loc;
      }
      return error;
    }
    exports.augmentError = augmentError;
    var Loc = class {
      constructor(line, column) {
        this.line = line;
        this.column = column;
      }
    };
    exports.Loc = Loc;
    function locationForIndex(pos) {
      let line = 1;
      let column = 1;
      for (let i = 0; i < pos; i++) {
        if (exports.input.charCodeAt(i) === _charcodes.charCodes.lineFeed) {
          line++;
          column = 1;
        } else {
          column++;
        }
      }
      return new Loc(line, column);
    }
    exports.locationForIndex = locationForIndex;
    function initParser(inputCode, isJSXEnabledArg, isTypeScriptEnabledArg, isFlowEnabledArg) {
      exports.input = inputCode;
      exports.state = new (0, _state2.default)();
      exports.nextContextId = 1;
      exports.isJSXEnabled = isJSXEnabledArg;
      exports.isTypeScriptEnabled = isTypeScriptEnabledArg;
      exports.isFlowEnabled = isFlowEnabledArg;
    }
    exports.initParser = initParser;
  }
});

// node_modules/sucrase/dist/parser/traverser/util.js
var require_util = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/util.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _types = require_types();
    var _charcodes = require_charcodes();
    var _base = require_base();
    function isContextual(contextualKeyword) {
      return _base.state.contextualKeyword === contextualKeyword;
    }
    exports.isContextual = isContextual;
    function isLookaheadContextual(contextualKeyword) {
      const l = _index.lookaheadTypeAndKeyword.call(void 0);
      return l.type === _types.TokenType.name && l.contextualKeyword === contextualKeyword;
    }
    exports.isLookaheadContextual = isLookaheadContextual;
    function eatContextual(contextualKeyword) {
      return _base.state.contextualKeyword === contextualKeyword && _index.eat.call(void 0, _types.TokenType.name);
    }
    exports.eatContextual = eatContextual;
    function expectContextual(contextualKeyword) {
      if (!eatContextual(contextualKeyword)) {
        unexpected();
      }
    }
    exports.expectContextual = expectContextual;
    function canInsertSemicolon() {
      return _index.match.call(void 0, _types.TokenType.eof) || _index.match.call(void 0, _types.TokenType.braceR) || hasPrecedingLineBreak();
    }
    exports.canInsertSemicolon = canInsertSemicolon;
    function hasPrecedingLineBreak() {
      const prevToken = _base.state.tokens[_base.state.tokens.length - 1];
      const lastTokEnd = prevToken ? prevToken.end : 0;
      for (let i = lastTokEnd; i < _base.state.start; i++) {
        const code = _base.input.charCodeAt(i);
        if (code === _charcodes.charCodes.lineFeed || code === _charcodes.charCodes.carriageReturn || code === 8232 || code === 8233) {
          return true;
        }
      }
      return false;
    }
    exports.hasPrecedingLineBreak = hasPrecedingLineBreak;
    function hasFollowingLineBreak() {
      const nextStart = _index.nextTokenStart.call(void 0);
      for (let i = _base.state.end; i < nextStart; i++) {
        const code = _base.input.charCodeAt(i);
        if (code === _charcodes.charCodes.lineFeed || code === _charcodes.charCodes.carriageReturn || code === 8232 || code === 8233) {
          return true;
        }
      }
      return false;
    }
    exports.hasFollowingLineBreak = hasFollowingLineBreak;
    function isLineTerminator() {
      return _index.eat.call(void 0, _types.TokenType.semi) || canInsertSemicolon();
    }
    exports.isLineTerminator = isLineTerminator;
    function semicolon() {
      if (!isLineTerminator()) {
        unexpected('Unexpected token, expected ";"');
      }
    }
    exports.semicolon = semicolon;
    function expect(type) {
      const matched = _index.eat.call(void 0, type);
      if (!matched) {
        unexpected(`Unexpected token, expected "${_types.formatTokenType.call(void 0, type)}"`);
      }
    }
    exports.expect = expect;
    function unexpected(message = "Unexpected token", pos = _base.state.start) {
      if (_base.state.error) {
        return;
      }
      const err = new SyntaxError(message);
      err.pos = pos;
      _base.state.error = err;
      _base.state.pos = _base.input.length;
      _index.finishToken.call(void 0, _types.TokenType.eof);
    }
    exports.unexpected = unexpected;
  }
});

// node_modules/sucrase/dist/parser/util/whitespace.js
var require_whitespace = __commonJS({
  "node_modules/sucrase/dist/parser/util/whitespace.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _charcodes = require_charcodes();
    var WHITESPACE_CHARS = [
      9,
      11,
      12,
      _charcodes.charCodes.space,
      _charcodes.charCodes.nonBreakingSpace,
      _charcodes.charCodes.oghamSpaceMark,
      8192,
      // EN QUAD
      8193,
      // EM QUAD
      8194,
      // EN SPACE
      8195,
      // EM SPACE
      8196,
      // THREE-PER-EM SPACE
      8197,
      // FOUR-PER-EM SPACE
      8198,
      // SIX-PER-EM SPACE
      8199,
      // FIGURE SPACE
      8200,
      // PUNCTUATION SPACE
      8201,
      // THIN SPACE
      8202,
      // HAIR SPACE
      8239,
      // NARROW NO-BREAK SPACE
      8287,
      // MEDIUM MATHEMATICAL SPACE
      12288,
      // IDEOGRAPHIC SPACE
      65279
      // ZERO WIDTH NO-BREAK SPACE
    ];
    exports.WHITESPACE_CHARS = WHITESPACE_CHARS;
    var skipWhiteSpace2 = /(?:\s|\/\/.*|\/\*[^]*?\*\/)*/g;
    exports.skipWhiteSpace = skipWhiteSpace2;
    var IS_WHITESPACE = new Uint8Array(65536);
    exports.IS_WHITESPACE = IS_WHITESPACE;
    for (const char of exports.WHITESPACE_CHARS) {
      exports.IS_WHITESPACE[char] = 1;
    }
  }
});

// node_modules/sucrase/dist/parser/util/identifier.js
var require_identifier = __commonJS({
  "node_modules/sucrase/dist/parser/util/identifier.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _charcodes = require_charcodes();
    var _whitespace = require_whitespace();
    function computeIsIdentifierChar(code) {
      if (code < 48) return code === 36;
      if (code < 58) return true;
      if (code < 65) return false;
      if (code < 91) return true;
      if (code < 97) return code === 95;
      if (code < 123) return true;
      if (code < 128) return false;
      throw new Error("Should not be called with non-ASCII char code.");
    }
    var IS_IDENTIFIER_CHAR = new Uint8Array(65536);
    exports.IS_IDENTIFIER_CHAR = IS_IDENTIFIER_CHAR;
    for (let i = 0; i < 128; i++) {
      exports.IS_IDENTIFIER_CHAR[i] = computeIsIdentifierChar(i) ? 1 : 0;
    }
    for (let i = 128; i < 65536; i++) {
      exports.IS_IDENTIFIER_CHAR[i] = 1;
    }
    for (const whitespaceChar of _whitespace.WHITESPACE_CHARS) {
      exports.IS_IDENTIFIER_CHAR[whitespaceChar] = 0;
    }
    exports.IS_IDENTIFIER_CHAR[8232] = 0;
    exports.IS_IDENTIFIER_CHAR[8233] = 0;
    var IS_IDENTIFIER_START = exports.IS_IDENTIFIER_CHAR.slice();
    exports.IS_IDENTIFIER_START = IS_IDENTIFIER_START;
    for (let numChar = _charcodes.charCodes.digit0; numChar <= _charcodes.charCodes.digit9; numChar++) {
      exports.IS_IDENTIFIER_START[numChar] = 0;
    }
  }
});

// node_modules/sucrase/dist/parser/tokenizer/readWordTree.js
var require_readWordTree = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/readWordTree.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    var _types = require_types();
    var READ_WORD_TREE = new Int32Array([
      // ""
      -1,
      27,
      783,
      918,
      1755,
      2376,
      2862,
      3483,
      -1,
      3699,
      -1,
      4617,
      4752,
      4833,
      5130,
      5508,
      5940,
      -1,
      6480,
      6939,
      7749,
      8181,
      8451,
      8613,
      -1,
      8829,
      -1,
      // "a"
      -1,
      -1,
      54,
      243,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      432,
      -1,
      -1,
      -1,
      675,
      -1,
      -1,
      -1,
      // "ab"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      81,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abs"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      108,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abst"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      135,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abstr"
      -1,
      162,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abstra"
      -1,
      -1,
      -1,
      189,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abstrac"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      216,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "abstract"
      _keywords.ContextualKeyword._abstract << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ac"
      -1,
      -1,
      -1,
      270,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "acc"
      -1,
      -1,
      -1,
      -1,
      -1,
      297,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "acce"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      324,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "acces"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      351,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "access"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      378,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "accesso"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      405,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "accessor"
      _keywords.ContextualKeyword._accessor << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "as"
      _keywords.ContextualKeyword._as << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      459,
      -1,
      -1,
      -1,
      -1,
      -1,
      594,
      -1,
      // "ass"
      -1,
      -1,
      -1,
      -1,
      -1,
      486,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "asse"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      513,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "asser"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      540,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "assert"
      _keywords.ContextualKeyword._assert << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      567,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "asserts"
      _keywords.ContextualKeyword._asserts << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "asy"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      621,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "asyn"
      -1,
      -1,
      -1,
      648,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "async"
      _keywords.ContextualKeyword._async << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "aw"
      -1,
      702,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "awa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      729,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "awai"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      756,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "await"
      _keywords.ContextualKeyword._await << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "b"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      810,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "br"
      -1,
      -1,
      -1,
      -1,
      -1,
      837,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "bre"
      -1,
      864,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "brea"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      891,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "break"
      (_types.TokenType._break << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "c"
      -1,
      945,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1107,
      -1,
      -1,
      -1,
      1242,
      -1,
      -1,
      1350,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ca"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      972,
      1026,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cas"
      -1,
      -1,
      -1,
      -1,
      -1,
      999,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "case"
      (_types.TokenType._case << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cat"
      -1,
      -1,
      -1,
      1053,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "catc"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1080,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "catch"
      (_types.TokenType._catch << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ch"
      -1,
      -1,
      -1,
      -1,
      -1,
      1134,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "che"
      -1,
      -1,
      -1,
      1161,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "chec"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1188,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "check"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1215,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "checks"
      _keywords.ContextualKeyword._checks << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cl"
      -1,
      1269,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cla"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1296,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "clas"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1323,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "class"
      (_types.TokenType._class << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "co"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1377,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "con"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1404,
      1620,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cons"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1431,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "const"
      (_types.TokenType._const << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1458,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "constr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1485,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "constru"
      -1,
      -1,
      -1,
      1512,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "construc"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1539,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "construct"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1566,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "constructo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1593,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "constructor"
      _keywords.ContextualKeyword._constructor << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "cont"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1647,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "conti"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1674,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "contin"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1701,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "continu"
      -1,
      -1,
      -1,
      -1,
      -1,
      1728,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "continue"
      (_types.TokenType._continue << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "d"
      -1,
      -1,
      -1,
      -1,
      -1,
      1782,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2349,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "de"
      -1,
      -1,
      1809,
      1971,
      -1,
      -1,
      2106,
      -1,
      -1,
      -1,
      -1,
      -1,
      2241,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "deb"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1836,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "debu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1863,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "debug"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1890,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "debugg"
      -1,
      -1,
      -1,
      -1,
      -1,
      1917,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "debugge"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1944,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "debugger"
      (_types.TokenType._debugger << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "dec"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      1998,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "decl"
      -1,
      2025,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "decla"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2052,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "declar"
      -1,
      -1,
      -1,
      -1,
      -1,
      2079,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "declare"
      _keywords.ContextualKeyword._declare << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "def"
      -1,
      2133,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "defa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2160,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "defau"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2187,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "defaul"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2214,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "default"
      (_types.TokenType._default << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "del"
      -1,
      -1,
      -1,
      -1,
      -1,
      2268,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "dele"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2295,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "delet"
      -1,
      -1,
      -1,
      -1,
      -1,
      2322,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "delete"
      (_types.TokenType._delete << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "do"
      (_types.TokenType._do << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "e"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2403,
      -1,
      2484,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2565,
      -1,
      -1,
      // "el"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2430,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "els"
      -1,
      -1,
      -1,
      -1,
      -1,
      2457,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "else"
      (_types.TokenType._else << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "en"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2511,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "enu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2538,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "enum"
      _keywords.ContextualKeyword._enum << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ex"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2592,
      -1,
      -1,
      -1,
      2727,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "exp"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2619,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "expo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2646,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "expor"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2673,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "export"
      (_types.TokenType._export << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2700,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "exports"
      _keywords.ContextualKeyword._exports << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ext"
      -1,
      -1,
      -1,
      -1,
      -1,
      2754,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "exte"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2781,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "exten"
      -1,
      -1,
      -1,
      -1,
      2808,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "extend"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2835,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "extends"
      (_types.TokenType._extends << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "f"
      -1,
      2889,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2997,
      -1,
      -1,
      -1,
      -1,
      -1,
      3159,
      -1,
      -1,
      3213,
      -1,
      -1,
      3294,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2916,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fal"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      2943,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fals"
      -1,
      -1,
      -1,
      -1,
      -1,
      2970,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "false"
      (_types.TokenType._false << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3024,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fin"
      -1,
      3051,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fina"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3078,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "final"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3105,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "finall"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3132,
      -1,
      // "finally"
      (_types.TokenType._finally << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3186,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "for"
      (_types.TokenType._for << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3240,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fro"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3267,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "from"
      _keywords.ContextualKeyword._from << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3321,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "fun"
      -1,
      -1,
      -1,
      3348,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "func"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3375,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "funct"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3402,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "functi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3429,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "functio"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3456,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "function"
      (_types.TokenType._function << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "g"
      -1,
      -1,
      -1,
      -1,
      -1,
      3510,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3564,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ge"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3537,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "get"
      _keywords.ContextualKeyword._get << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "gl"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3591,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "glo"
      -1,
      -1,
      3618,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "glob"
      -1,
      3645,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "globa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3672,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "global"
      _keywords.ContextualKeyword._global << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "i"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3726,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3753,
      4077,
      -1,
      -1,
      -1,
      -1,
      4590,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "if"
      (_types.TokenType._if << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "im"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3780,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "imp"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3807,
      -1,
      -1,
      3996,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "impl"
      -1,
      -1,
      -1,
      -1,
      -1,
      3834,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "imple"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3861,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "implem"
      -1,
      -1,
      -1,
      -1,
      -1,
      3888,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "impleme"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3915,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "implemen"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3942,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "implement"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      3969,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "implements"
      _keywords.ContextualKeyword._implements << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "impo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4023,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "impor"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4050,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "import"
      (_types.TokenType._import << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "in"
      (_types.TokenType._in << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4104,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4185,
      4401,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "inf"
      -1,
      -1,
      -1,
      -1,
      -1,
      4131,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "infe"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4158,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "infer"
      _keywords.ContextualKeyword._infer << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ins"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4212,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "inst"
      -1,
      4239,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "insta"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4266,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "instan"
      -1,
      -1,
      -1,
      4293,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "instanc"
      -1,
      -1,
      -1,
      -1,
      -1,
      4320,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "instance"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4347,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "instanceo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4374,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "instanceof"
      (_types.TokenType._instanceof << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "int"
      -1,
      -1,
      -1,
      -1,
      -1,
      4428,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "inte"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4455,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "inter"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4482,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "interf"
      -1,
      4509,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "interfa"
      -1,
      -1,
      -1,
      4536,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "interfac"
      -1,
      -1,
      -1,
      -1,
      -1,
      4563,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "interface"
      _keywords.ContextualKeyword._interface << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "is"
      _keywords.ContextualKeyword._is << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "k"
      -1,
      -1,
      -1,
      -1,
      -1,
      4644,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ke"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4671,
      -1,
      // "key"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4698,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "keyo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4725,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "keyof"
      _keywords.ContextualKeyword._keyof << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "l"
      -1,
      -1,
      -1,
      -1,
      -1,
      4779,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "le"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4806,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "let"
      (_types.TokenType._let << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "m"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4860,
      -1,
      -1,
      -1,
      -1,
      -1,
      4995,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4887,
      -1,
      -1,
      // "mix"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4914,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mixi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4941,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mixin"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      4968,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mixins"
      _keywords.ContextualKeyword._mixins << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mo"
      -1,
      -1,
      -1,
      -1,
      5022,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "mod"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5049,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "modu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5076,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "modul"
      -1,
      -1,
      -1,
      -1,
      -1,
      5103,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "module"
      _keywords.ContextualKeyword._module << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "n"
      -1,
      5157,
      -1,
      -1,
      -1,
      5373,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5427,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "na"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5184,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "nam"
      -1,
      -1,
      -1,
      -1,
      -1,
      5211,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "name"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5238,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "names"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5265,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "namesp"
      -1,
      5292,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "namespa"
      -1,
      -1,
      -1,
      5319,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "namespac"
      -1,
      -1,
      -1,
      -1,
      -1,
      5346,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "namespace"
      _keywords.ContextualKeyword._namespace << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ne"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5400,
      -1,
      -1,
      -1,
      // "new"
      (_types.TokenType._new << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "nu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5454,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "nul"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5481,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "null"
      (_types.TokenType._null << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "o"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5535,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5562,
      -1,
      -1,
      -1,
      -1,
      5697,
      5751,
      -1,
      -1,
      -1,
      -1,
      // "of"
      _keywords.ContextualKeyword._of << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "op"
      -1,
      5589,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "opa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5616,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "opaq"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5643,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "opaqu"
      -1,
      -1,
      -1,
      -1,
      -1,
      5670,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "opaque"
      _keywords.ContextualKeyword._opaque << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ou"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5724,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "out"
      _keywords.ContextualKeyword._out << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ov"
      -1,
      -1,
      -1,
      -1,
      -1,
      5778,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ove"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5805,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "over"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5832,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "overr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5859,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "overri"
      -1,
      -1,
      -1,
      -1,
      5886,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "overrid"
      -1,
      -1,
      -1,
      -1,
      -1,
      5913,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "override"
      _keywords.ContextualKeyword._override << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "p"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5967,
      -1,
      -1,
      6345,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "pr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      5994,
      -1,
      -1,
      -1,
      -1,
      -1,
      6129,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "pri"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6021,
      -1,
      -1,
      -1,
      -1,
      // "priv"
      -1,
      6048,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "priva"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6075,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "privat"
      -1,
      -1,
      -1,
      -1,
      -1,
      6102,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "private"
      _keywords.ContextualKeyword._private << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "pro"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6156,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "prot"
      -1,
      -1,
      -1,
      -1,
      -1,
      6183,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6318,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "prote"
      -1,
      -1,
      -1,
      6210,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "protec"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6237,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "protect"
      -1,
      -1,
      -1,
      -1,
      -1,
      6264,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "protecte"
      -1,
      -1,
      -1,
      -1,
      6291,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "protected"
      _keywords.ContextualKeyword._protected << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "proto"
      _keywords.ContextualKeyword._proto << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "pu"
      -1,
      -1,
      6372,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "pub"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6399,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "publ"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6426,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "publi"
      -1,
      -1,
      -1,
      6453,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "public"
      _keywords.ContextualKeyword._public << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "r"
      -1,
      -1,
      -1,
      -1,
      -1,
      6507,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "re"
      -1,
      6534,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6696,
      -1,
      -1,
      6831,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "rea"
      -1,
      -1,
      -1,
      -1,
      6561,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "read"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6588,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "reado"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6615,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "readon"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6642,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "readonl"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6669,
      -1,
      // "readonly"
      _keywords.ContextualKeyword._readonly << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "req"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6723,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "requ"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6750,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "requi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6777,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "requir"
      -1,
      -1,
      -1,
      -1,
      -1,
      6804,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "require"
      _keywords.ContextualKeyword._require << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ret"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6858,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "retu"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6885,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "retur"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6912,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "return"
      (_types.TokenType._return << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "s"
      -1,
      6966,
      -1,
      -1,
      -1,
      7182,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7236,
      7371,
      -1,
      7479,
      -1,
      7614,
      -1,
      // "sa"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      6993,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sat"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7020,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sati"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7047,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "satis"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7074,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "satisf"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7101,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "satisfi"
      -1,
      -1,
      -1,
      -1,
      -1,
      7128,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "satisfie"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7155,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "satisfies"
      _keywords.ContextualKeyword._satisfies << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "se"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7209,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "set"
      _keywords.ContextualKeyword._set << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "st"
      -1,
      7263,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sta"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7290,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "stat"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7317,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "stati"
      -1,
      -1,
      -1,
      7344,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "static"
      _keywords.ContextualKeyword._static << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "su"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7398,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sup"
      -1,
      -1,
      -1,
      -1,
      -1,
      7425,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "supe"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7452,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "super"
      (_types.TokenType._super << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sw"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7506,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "swi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7533,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "swit"
      -1,
      -1,
      -1,
      7560,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "switc"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7587,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "switch"
      (_types.TokenType._switch << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sy"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7641,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "sym"
      -1,
      -1,
      7668,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "symb"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7695,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "symbo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7722,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "symbol"
      _keywords.ContextualKeyword._symbol << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "t"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7776,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7938,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8046,
      -1,
      // "th"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7803,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7857,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "thi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7830,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "this"
      (_types.TokenType._this << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "thr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7884,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "thro"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7911,
      -1,
      -1,
      -1,
      // "throw"
      (_types.TokenType._throw << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "tr"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      7965,
      -1,
      -1,
      -1,
      8019,
      -1,
      // "tru"
      -1,
      -1,
      -1,
      -1,
      -1,
      7992,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "true"
      (_types.TokenType._true << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "try"
      (_types.TokenType._try << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "ty"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8073,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "typ"
      -1,
      -1,
      -1,
      -1,
      -1,
      8100,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "type"
      _keywords.ContextualKeyword._type << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8127,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "typeo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8154,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "typeof"
      (_types.TokenType._typeof << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "u"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8208,
      -1,
      -1,
      -1,
      -1,
      8343,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "un"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8235,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "uni"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8262,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "uniq"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8289,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "uniqu"
      -1,
      -1,
      -1,
      -1,
      -1,
      8316,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "unique"
      _keywords.ContextualKeyword._unique << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "us"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8370,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "usi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8397,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "usin"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8424,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "using"
      _keywords.ContextualKeyword._using << 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "v"
      -1,
      8478,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8532,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "va"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8505,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "var"
      (_types.TokenType._var << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "vo"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8559,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "voi"
      -1,
      -1,
      -1,
      -1,
      8586,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "void"
      (_types.TokenType._void << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "w"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8640,
      8748,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "wh"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8667,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "whi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8694,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "whil"
      -1,
      -1,
      -1,
      -1,
      -1,
      8721,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "while"
      (_types.TokenType._while << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "wi"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8775,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "wit"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8802,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "with"
      (_types.TokenType._with << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "y"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8856,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "yi"
      -1,
      -1,
      -1,
      -1,
      -1,
      8883,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "yie"
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      8910,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "yiel"
      -1,
      -1,
      -1,
      -1,
      8937,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      // "yield"
      (_types.TokenType._yield << 1) + 1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1
    ]);
    exports.READ_WORD_TREE = READ_WORD_TREE;
  }
});

// node_modules/sucrase/dist/parser/tokenizer/readWord.js
var require_readWord = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/readWord.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _base = require_base();
    var _charcodes = require_charcodes();
    var _identifier = require_identifier();
    var _index = require_tokenizer();
    var _readWordTree = require_readWordTree();
    var _types = require_types();
    function readWord() {
      let treePos = 0;
      let code = 0;
      let pos = _base.state.pos;
      while (pos < _base.input.length) {
        code = _base.input.charCodeAt(pos);
        if (code < _charcodes.charCodes.lowercaseA || code > _charcodes.charCodes.lowercaseZ) {
          break;
        }
        const next = _readWordTree.READ_WORD_TREE[treePos + (code - _charcodes.charCodes.lowercaseA) + 1];
        if (next === -1) {
          break;
        } else {
          treePos = next;
          pos++;
        }
      }
      const keywordValue = _readWordTree.READ_WORD_TREE[treePos];
      if (keywordValue > -1 && !_identifier.IS_IDENTIFIER_CHAR[code]) {
        _base.state.pos = pos;
        if (keywordValue & 1) {
          _index.finishToken.call(void 0, keywordValue >>> 1);
        } else {
          _index.finishToken.call(void 0, _types.TokenType.name, keywordValue >>> 1);
        }
        return;
      }
      while (pos < _base.input.length) {
        const ch = _base.input.charCodeAt(pos);
        if (_identifier.IS_IDENTIFIER_CHAR[ch]) {
          pos++;
        } else if (ch === _charcodes.charCodes.backslash) {
          pos += 2;
          if (_base.input.charCodeAt(pos) === _charcodes.charCodes.leftCurlyBrace) {
            while (pos < _base.input.length && _base.input.charCodeAt(pos) !== _charcodes.charCodes.rightCurlyBrace) {
              pos++;
            }
            pos++;
          }
        } else if (ch === _charcodes.charCodes.atSign && _base.input.charCodeAt(pos + 1) === _charcodes.charCodes.atSign) {
          pos += 2;
        } else {
          break;
        }
      }
      _base.state.pos = pos;
      _index.finishToken.call(void 0, _types.TokenType.name);
    }
    exports.default = readWord;
  }
});

// node_modules/sucrase/dist/parser/tokenizer/index.js
var require_tokenizer = __commonJS({
  "node_modules/sucrase/dist/parser/tokenizer/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _base = require_base();
    var _util = require_util();
    var _charcodes = require_charcodes();
    var _identifier = require_identifier();
    var _whitespace = require_whitespace();
    var _keywords = require_keywords();
    var _readWord = require_readWord();
    var _readWord2 = _interopRequireDefault(_readWord);
    var _types = require_types();
    var IdentifierRole;
    (function(IdentifierRole2) {
      const Access = 0;
      IdentifierRole2[IdentifierRole2["Access"] = Access] = "Access";
      const ExportAccess = Access + 1;
      IdentifierRole2[IdentifierRole2["ExportAccess"] = ExportAccess] = "ExportAccess";
      const TopLevelDeclaration = ExportAccess + 1;
      IdentifierRole2[IdentifierRole2["TopLevelDeclaration"] = TopLevelDeclaration] = "TopLevelDeclaration";
      const FunctionScopedDeclaration = TopLevelDeclaration + 1;
      IdentifierRole2[IdentifierRole2["FunctionScopedDeclaration"] = FunctionScopedDeclaration] = "FunctionScopedDeclaration";
      const BlockScopedDeclaration = FunctionScopedDeclaration + 1;
      IdentifierRole2[IdentifierRole2["BlockScopedDeclaration"] = BlockScopedDeclaration] = "BlockScopedDeclaration";
      const ObjectShorthandTopLevelDeclaration = BlockScopedDeclaration + 1;
      IdentifierRole2[IdentifierRole2["ObjectShorthandTopLevelDeclaration"] = ObjectShorthandTopLevelDeclaration] = "ObjectShorthandTopLevelDeclaration";
      const ObjectShorthandFunctionScopedDeclaration = ObjectShorthandTopLevelDeclaration + 1;
      IdentifierRole2[IdentifierRole2["ObjectShorthandFunctionScopedDeclaration"] = ObjectShorthandFunctionScopedDeclaration] = "ObjectShorthandFunctionScopedDeclaration";
      const ObjectShorthandBlockScopedDeclaration = ObjectShorthandFunctionScopedDeclaration + 1;
      IdentifierRole2[IdentifierRole2["ObjectShorthandBlockScopedDeclaration"] = ObjectShorthandBlockScopedDeclaration] = "ObjectShorthandBlockScopedDeclaration";
      const ObjectShorthand = ObjectShorthandBlockScopedDeclaration + 1;
      IdentifierRole2[IdentifierRole2["ObjectShorthand"] = ObjectShorthand] = "ObjectShorthand";
      const ImportDeclaration = ObjectShorthand + 1;
      IdentifierRole2[IdentifierRole2["ImportDeclaration"] = ImportDeclaration] = "ImportDeclaration";
      const ObjectKey = ImportDeclaration + 1;
      IdentifierRole2[IdentifierRole2["ObjectKey"] = ObjectKey] = "ObjectKey";
      const ImportAccess = ObjectKey + 1;
      IdentifierRole2[IdentifierRole2["ImportAccess"] = ImportAccess] = "ImportAccess";
    })(IdentifierRole || (exports.IdentifierRole = IdentifierRole = {}));
    var JSXRole;
    (function(JSXRole2) {
      const NoChildren = 0;
      JSXRole2[JSXRole2["NoChildren"] = NoChildren] = "NoChildren";
      const OneChild = NoChildren + 1;
      JSXRole2[JSXRole2["OneChild"] = OneChild] = "OneChild";
      const StaticChildren = OneChild + 1;
      JSXRole2[JSXRole2["StaticChildren"] = StaticChildren] = "StaticChildren";
      const KeyAfterPropSpread = StaticChildren + 1;
      JSXRole2[JSXRole2["KeyAfterPropSpread"] = KeyAfterPropSpread] = "KeyAfterPropSpread";
    })(JSXRole || (exports.JSXRole = JSXRole = {}));
    function isDeclaration(token) {
      const role = token.identifierRole;
      return role === IdentifierRole.TopLevelDeclaration || role === IdentifierRole.FunctionScopedDeclaration || role === IdentifierRole.BlockScopedDeclaration || role === IdentifierRole.ObjectShorthandTopLevelDeclaration || role === IdentifierRole.ObjectShorthandFunctionScopedDeclaration || role === IdentifierRole.ObjectShorthandBlockScopedDeclaration;
    }
    exports.isDeclaration = isDeclaration;
    function isNonTopLevelDeclaration(token) {
      const role = token.identifierRole;
      return role === IdentifierRole.FunctionScopedDeclaration || role === IdentifierRole.BlockScopedDeclaration || role === IdentifierRole.ObjectShorthandFunctionScopedDeclaration || role === IdentifierRole.ObjectShorthandBlockScopedDeclaration;
    }
    exports.isNonTopLevelDeclaration = isNonTopLevelDeclaration;
    function isTopLevelDeclaration(token) {
      const role = token.identifierRole;
      return role === IdentifierRole.TopLevelDeclaration || role === IdentifierRole.ObjectShorthandTopLevelDeclaration || role === IdentifierRole.ImportDeclaration;
    }
    exports.isTopLevelDeclaration = isTopLevelDeclaration;
    function isBlockScopedDeclaration(token) {
      const role = token.identifierRole;
      return role === IdentifierRole.TopLevelDeclaration || role === IdentifierRole.BlockScopedDeclaration || role === IdentifierRole.ObjectShorthandTopLevelDeclaration || role === IdentifierRole.ObjectShorthandBlockScopedDeclaration;
    }
    exports.isBlockScopedDeclaration = isBlockScopedDeclaration;
    function isFunctionScopedDeclaration(token) {
      const role = token.identifierRole;
      return role === IdentifierRole.FunctionScopedDeclaration || role === IdentifierRole.ObjectShorthandFunctionScopedDeclaration;
    }
    exports.isFunctionScopedDeclaration = isFunctionScopedDeclaration;
    function isObjectShorthandDeclaration(token) {
      return token.identifierRole === IdentifierRole.ObjectShorthandTopLevelDeclaration || token.identifierRole === IdentifierRole.ObjectShorthandBlockScopedDeclaration || token.identifierRole === IdentifierRole.ObjectShorthandFunctionScopedDeclaration;
    }
    exports.isObjectShorthandDeclaration = isObjectShorthandDeclaration;
    var Token3 = class {
      constructor() {
        this.type = _base.state.type;
        this.contextualKeyword = _base.state.contextualKeyword;
        this.start = _base.state.start;
        this.end = _base.state.end;
        this.scopeDepth = _base.state.scopeDepth;
        this.isType = _base.state.isType;
        this.identifierRole = null;
        this.jsxRole = null;
        this.shadowsGlobal = false;
        this.isAsyncOperation = false;
        this.contextId = null;
        this.rhsEndIndex = null;
        this.isExpression = false;
        this.numNullishCoalesceStarts = 0;
        this.numNullishCoalesceEnds = 0;
        this.isOptionalChainStart = false;
        this.isOptionalChainEnd = false;
        this.subscriptStartIndex = null;
        this.nullishStartIndex = null;
      }
      // Initially false for all tokens, then may be computed in a follow-up step that does scope
      // analysis.
      // Initially false for all tokens, but may be set during transform to mark it as containing an
      // await operation.
      // For assignments, the index of the RHS. For export tokens, the end of the export.
      // For class tokens, records if the class is a class expression or a class statement.
      // Number of times to insert a `nullishCoalesce(` snippet before this token.
      // Number of times to insert a `)` snippet after this token.
      // If true, insert an `optionalChain([` snippet before this token.
      // If true, insert a `])` snippet after this token.
      // Tag for `.`, `?.`, `[`, `?.[`, `(`, and `?.(` to denote the "root" token for this
      // subscript chain. This can be used to determine if this chain is an optional chain.
      // Tag for `??` operators to denote the root token for this nullish coalescing call.
    };
    exports.Token = Token3;
    function next() {
      _base.state.tokens.push(new Token3());
      nextToken();
    }
    exports.next = next;
    function nextTemplateToken() {
      _base.state.tokens.push(new Token3());
      _base.state.start = _base.state.pos;
      readTmplToken();
    }
    exports.nextTemplateToken = nextTemplateToken;
    function retokenizeSlashAsRegex() {
      if (_base.state.type === _types.TokenType.assign) {
        --_base.state.pos;
      }
      readRegexp();
    }
    exports.retokenizeSlashAsRegex = retokenizeSlashAsRegex;
    function pushTypeContext(existingTokensInType) {
      for (let i = _base.state.tokens.length - existingTokensInType; i < _base.state.tokens.length; i++) {
        _base.state.tokens[i].isType = true;
      }
      const oldIsType = _base.state.isType;
      _base.state.isType = true;
      return oldIsType;
    }
    exports.pushTypeContext = pushTypeContext;
    function popTypeContext(oldIsType) {
      _base.state.isType = oldIsType;
    }
    exports.popTypeContext = popTypeContext;
    function eat2(type) {
      if (match(type)) {
        next();
        return true;
      } else {
        return false;
      }
    }
    exports.eat = eat2;
    function eatTypeToken(tokenType) {
      const oldIsType = _base.state.isType;
      _base.state.isType = true;
      eat2(tokenType);
      _base.state.isType = oldIsType;
    }
    exports.eatTypeToken = eatTypeToken;
    function match(type) {
      return _base.state.type === type;
    }
    exports.match = match;
    function lookaheadType() {
      const snapshot = _base.state.snapshot();
      next();
      const type = _base.state.type;
      _base.state.restoreFromSnapshot(snapshot);
      return type;
    }
    exports.lookaheadType = lookaheadType;
    var TypeAndKeyword = class {
      constructor(type, contextualKeyword) {
        this.type = type;
        this.contextualKeyword = contextualKeyword;
      }
    };
    exports.TypeAndKeyword = TypeAndKeyword;
    function lookaheadTypeAndKeyword() {
      const snapshot = _base.state.snapshot();
      next();
      const type = _base.state.type;
      const contextualKeyword = _base.state.contextualKeyword;
      _base.state.restoreFromSnapshot(snapshot);
      return new TypeAndKeyword(type, contextualKeyword);
    }
    exports.lookaheadTypeAndKeyword = lookaheadTypeAndKeyword;
    function nextTokenStart() {
      return nextTokenStartSince(_base.state.pos);
    }
    exports.nextTokenStart = nextTokenStart;
    function nextTokenStartSince(pos) {
      _whitespace.skipWhiteSpace.lastIndex = pos;
      const skip = _whitespace.skipWhiteSpace.exec(_base.input);
      return pos + skip[0].length;
    }
    exports.nextTokenStartSince = nextTokenStartSince;
    function lookaheadCharCode() {
      return _base.input.charCodeAt(nextTokenStart());
    }
    exports.lookaheadCharCode = lookaheadCharCode;
    function nextToken() {
      skipSpace();
      _base.state.start = _base.state.pos;
      if (_base.state.pos >= _base.input.length) {
        const tokens = _base.state.tokens;
        if (tokens.length >= 2 && tokens[tokens.length - 1].start >= _base.input.length && tokens[tokens.length - 2].start >= _base.input.length) {
          _util.unexpected.call(void 0, "Unexpectedly reached the end of input.");
        }
        finishToken(_types.TokenType.eof);
        return;
      }
      readToken(_base.input.charCodeAt(_base.state.pos));
    }
    exports.nextToken = nextToken;
    function readToken(code) {
      if (_identifier.IS_IDENTIFIER_START[code] || code === _charcodes.charCodes.backslash || code === _charcodes.charCodes.atSign && _base.input.charCodeAt(_base.state.pos + 1) === _charcodes.charCodes.atSign) {
        _readWord2.default.call(void 0);
      } else {
        getTokenFromCode(code);
      }
    }
    function skipBlockComment() {
      while (_base.input.charCodeAt(_base.state.pos) !== _charcodes.charCodes.asterisk || _base.input.charCodeAt(_base.state.pos + 1) !== _charcodes.charCodes.slash) {
        _base.state.pos++;
        if (_base.state.pos > _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated comment", _base.state.pos - 2);
          return;
        }
      }
      _base.state.pos += 2;
    }
    function skipLineComment(startSkip) {
      let ch = _base.input.charCodeAt(_base.state.pos += startSkip);
      if (_base.state.pos < _base.input.length) {
        while (ch !== _charcodes.charCodes.lineFeed && ch !== _charcodes.charCodes.carriageReturn && ch !== _charcodes.charCodes.lineSeparator && ch !== _charcodes.charCodes.paragraphSeparator && ++_base.state.pos < _base.input.length) {
          ch = _base.input.charCodeAt(_base.state.pos);
        }
      }
    }
    exports.skipLineComment = skipLineComment;
    function skipSpace() {
      while (_base.state.pos < _base.input.length) {
        const ch = _base.input.charCodeAt(_base.state.pos);
        switch (ch) {
          case _charcodes.charCodes.carriageReturn:
            if (_base.input.charCodeAt(_base.state.pos + 1) === _charcodes.charCodes.lineFeed) {
              ++_base.state.pos;
            }
          case _charcodes.charCodes.lineFeed:
          case _charcodes.charCodes.lineSeparator:
          case _charcodes.charCodes.paragraphSeparator:
            ++_base.state.pos;
            break;
          case _charcodes.charCodes.slash:
            switch (_base.input.charCodeAt(_base.state.pos + 1)) {
              case _charcodes.charCodes.asterisk:
                _base.state.pos += 2;
                skipBlockComment();
                break;
              case _charcodes.charCodes.slash:
                skipLineComment(2);
                break;
              default:
                return;
            }
            break;
          default:
            if (_whitespace.IS_WHITESPACE[ch]) {
              ++_base.state.pos;
            } else {
              return;
            }
        }
      }
    }
    exports.skipSpace = skipSpace;
    function finishToken(type, contextualKeyword = _keywords.ContextualKeyword.NONE) {
      _base.state.end = _base.state.pos;
      _base.state.type = type;
      _base.state.contextualKeyword = contextualKeyword;
    }
    exports.finishToken = finishToken;
    function readToken_dot() {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar >= _charcodes.charCodes.digit0 && nextChar <= _charcodes.charCodes.digit9) {
        readNumber(true);
        return;
      }
      if (nextChar === _charcodes.charCodes.dot && _base.input.charCodeAt(_base.state.pos + 2) === _charcodes.charCodes.dot) {
        _base.state.pos += 3;
        finishToken(_types.TokenType.ellipsis);
      } else {
        ++_base.state.pos;
        finishToken(_types.TokenType.dot);
      }
    }
    function readToken_slash() {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.assign, 2);
      } else {
        finishOp(_types.TokenType.slash, 1);
      }
    }
    function readToken_mult_modulo(code) {
      let tokenType = code === _charcodes.charCodes.asterisk ? _types.TokenType.star : _types.TokenType.modulo;
      let width = 1;
      let nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (code === _charcodes.charCodes.asterisk && nextChar === _charcodes.charCodes.asterisk) {
        width++;
        nextChar = _base.input.charCodeAt(_base.state.pos + 2);
        tokenType = _types.TokenType.exponent;
      }
      if (nextChar === _charcodes.charCodes.equalsTo && _base.input.charCodeAt(_base.state.pos + 2) !== _charcodes.charCodes.greaterThan) {
        width++;
        tokenType = _types.TokenType.assign;
      }
      finishOp(tokenType, width);
    }
    function readToken_pipe_amp(code) {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === code) {
        if (_base.input.charCodeAt(_base.state.pos + 2) === _charcodes.charCodes.equalsTo) {
          finishOp(_types.TokenType.assign, 3);
        } else {
          finishOp(code === _charcodes.charCodes.verticalBar ? _types.TokenType.logicalOR : _types.TokenType.logicalAND, 2);
        }
        return;
      }
      if (code === _charcodes.charCodes.verticalBar) {
        if (nextChar === _charcodes.charCodes.greaterThan) {
          finishOp(_types.TokenType.pipeline, 2);
          return;
        } else if (nextChar === _charcodes.charCodes.rightCurlyBrace && _base.isFlowEnabled) {
          finishOp(_types.TokenType.braceBarR, 2);
          return;
        }
      }
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.assign, 2);
        return;
      }
      finishOp(code === _charcodes.charCodes.verticalBar ? _types.TokenType.bitwiseOR : _types.TokenType.bitwiseAND, 1);
    }
    function readToken_caret() {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.assign, 2);
      } else {
        finishOp(_types.TokenType.bitwiseXOR, 1);
      }
    }
    function readToken_plus_min(code) {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === code) {
        finishOp(_types.TokenType.preIncDec, 2);
        return;
      }
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.assign, 2);
      } else if (code === _charcodes.charCodes.plusSign) {
        finishOp(_types.TokenType.plus, 1);
      } else {
        finishOp(_types.TokenType.minus, 1);
      }
    }
    function readToken_lt() {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === _charcodes.charCodes.lessThan) {
        if (_base.input.charCodeAt(_base.state.pos + 2) === _charcodes.charCodes.equalsTo) {
          finishOp(_types.TokenType.assign, 3);
          return;
        }
        if (_base.state.isType) {
          finishOp(_types.TokenType.lessThan, 1);
        } else {
          finishOp(_types.TokenType.bitShiftL, 2);
        }
        return;
      }
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.relationalOrEqual, 2);
      } else {
        finishOp(_types.TokenType.lessThan, 1);
      }
    }
    function readToken_gt() {
      if (_base.state.isType) {
        finishOp(_types.TokenType.greaterThan, 1);
        return;
      }
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === _charcodes.charCodes.greaterThan) {
        const size = _base.input.charCodeAt(_base.state.pos + 2) === _charcodes.charCodes.greaterThan ? 3 : 2;
        if (_base.input.charCodeAt(_base.state.pos + size) === _charcodes.charCodes.equalsTo) {
          finishOp(_types.TokenType.assign, size + 1);
          return;
        }
        finishOp(_types.TokenType.bitShiftR, size);
        return;
      }
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.relationalOrEqual, 2);
      } else {
        finishOp(_types.TokenType.greaterThan, 1);
      }
    }
    function rescan_gt() {
      if (_base.state.type === _types.TokenType.greaterThan) {
        _base.state.pos -= 1;
        readToken_gt();
      }
    }
    exports.rescan_gt = rescan_gt;
    function readToken_eq_excl(code) {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      if (nextChar === _charcodes.charCodes.equalsTo) {
        finishOp(_types.TokenType.equality, _base.input.charCodeAt(_base.state.pos + 2) === _charcodes.charCodes.equalsTo ? 3 : 2);
        return;
      }
      if (code === _charcodes.charCodes.equalsTo && nextChar === _charcodes.charCodes.greaterThan) {
        _base.state.pos += 2;
        finishToken(_types.TokenType.arrow);
        return;
      }
      finishOp(code === _charcodes.charCodes.equalsTo ? _types.TokenType.eq : _types.TokenType.bang, 1);
    }
    function readToken_question() {
      const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
      const nextChar2 = _base.input.charCodeAt(_base.state.pos + 2);
      if (nextChar === _charcodes.charCodes.questionMark && // In Flow (but not TypeScript), ??string is a valid type that should be
      // tokenized as two individual ? tokens.
      !(_base.isFlowEnabled && _base.state.isType)) {
        if (nextChar2 === _charcodes.charCodes.equalsTo) {
          finishOp(_types.TokenType.assign, 3);
        } else {
          finishOp(_types.TokenType.nullishCoalescing, 2);
        }
      } else if (nextChar === _charcodes.charCodes.dot && !(nextChar2 >= _charcodes.charCodes.digit0 && nextChar2 <= _charcodes.charCodes.digit9)) {
        _base.state.pos += 2;
        finishToken(_types.TokenType.questionDot);
      } else {
        ++_base.state.pos;
        finishToken(_types.TokenType.question);
      }
    }
    function getTokenFromCode(code) {
      switch (code) {
        case _charcodes.charCodes.numberSign:
          ++_base.state.pos;
          finishToken(_types.TokenType.hash);
          return;
        // The interpretation of a dot depends on whether it is followed
        // by a digit or another two dots.
        case _charcodes.charCodes.dot:
          readToken_dot();
          return;
        // Punctuation tokens.
        case _charcodes.charCodes.leftParenthesis:
          ++_base.state.pos;
          finishToken(_types.TokenType.parenL);
          return;
        case _charcodes.charCodes.rightParenthesis:
          ++_base.state.pos;
          finishToken(_types.TokenType.parenR);
          return;
        case _charcodes.charCodes.semicolon:
          ++_base.state.pos;
          finishToken(_types.TokenType.semi);
          return;
        case _charcodes.charCodes.comma:
          ++_base.state.pos;
          finishToken(_types.TokenType.comma);
          return;
        case _charcodes.charCodes.leftSquareBracket:
          ++_base.state.pos;
          finishToken(_types.TokenType.bracketL);
          return;
        case _charcodes.charCodes.rightSquareBracket:
          ++_base.state.pos;
          finishToken(_types.TokenType.bracketR);
          return;
        case _charcodes.charCodes.leftCurlyBrace:
          if (_base.isFlowEnabled && _base.input.charCodeAt(_base.state.pos + 1) === _charcodes.charCodes.verticalBar) {
            finishOp(_types.TokenType.braceBarL, 2);
          } else {
            ++_base.state.pos;
            finishToken(_types.TokenType.braceL);
          }
          return;
        case _charcodes.charCodes.rightCurlyBrace:
          ++_base.state.pos;
          finishToken(_types.TokenType.braceR);
          return;
        case _charcodes.charCodes.colon:
          if (_base.input.charCodeAt(_base.state.pos + 1) === _charcodes.charCodes.colon) {
            finishOp(_types.TokenType.doubleColon, 2);
          } else {
            ++_base.state.pos;
            finishToken(_types.TokenType.colon);
          }
          return;
        case _charcodes.charCodes.questionMark:
          readToken_question();
          return;
        case _charcodes.charCodes.atSign:
          ++_base.state.pos;
          finishToken(_types.TokenType.at);
          return;
        case _charcodes.charCodes.graveAccent:
          ++_base.state.pos;
          finishToken(_types.TokenType.backQuote);
          return;
        case _charcodes.charCodes.digit0: {
          const nextChar = _base.input.charCodeAt(_base.state.pos + 1);
          if (nextChar === _charcodes.charCodes.lowercaseX || nextChar === _charcodes.charCodes.uppercaseX || nextChar === _charcodes.charCodes.lowercaseO || nextChar === _charcodes.charCodes.uppercaseO || nextChar === _charcodes.charCodes.lowercaseB || nextChar === _charcodes.charCodes.uppercaseB) {
            readRadixNumber();
            return;
          }
        }
        // Anything else beginning with a digit is an integer, octal
        // number, or float.
        case _charcodes.charCodes.digit1:
        case _charcodes.charCodes.digit2:
        case _charcodes.charCodes.digit3:
        case _charcodes.charCodes.digit4:
        case _charcodes.charCodes.digit5:
        case _charcodes.charCodes.digit6:
        case _charcodes.charCodes.digit7:
        case _charcodes.charCodes.digit8:
        case _charcodes.charCodes.digit9:
          readNumber(false);
          return;
        // Quotes produce strings.
        case _charcodes.charCodes.quotationMark:
        case _charcodes.charCodes.apostrophe:
          readString(code);
          return;
        // Operators are parsed inline in tiny state machines. '=' (charCodes.equalsTo) is
        // often referred to. `finishOp` simply skips the amount of
        // characters it is given as second argument, and returns a token
        // of the type given by its first argument.
        case _charcodes.charCodes.slash:
          readToken_slash();
          return;
        case _charcodes.charCodes.percentSign:
        case _charcodes.charCodes.asterisk:
          readToken_mult_modulo(code);
          return;
        case _charcodes.charCodes.verticalBar:
        case _charcodes.charCodes.ampersand:
          readToken_pipe_amp(code);
          return;
        case _charcodes.charCodes.caret:
          readToken_caret();
          return;
        case _charcodes.charCodes.plusSign:
        case _charcodes.charCodes.dash:
          readToken_plus_min(code);
          return;
        case _charcodes.charCodes.lessThan:
          readToken_lt();
          return;
        case _charcodes.charCodes.greaterThan:
          readToken_gt();
          return;
        case _charcodes.charCodes.equalsTo:
        case _charcodes.charCodes.exclamationMark:
          readToken_eq_excl(code);
          return;
        case _charcodes.charCodes.tilde:
          finishOp(_types.TokenType.tilde, 1);
          return;
        default:
          break;
      }
      _util.unexpected.call(void 0, `Unexpected character '${String.fromCharCode(code)}'`, _base.state.pos);
    }
    exports.getTokenFromCode = getTokenFromCode;
    function finishOp(type, size) {
      _base.state.pos += size;
      finishToken(type);
    }
    function readRegexp() {
      const start = _base.state.pos;
      let escaped = false;
      let inClass = false;
      for (; ; ) {
        if (_base.state.pos >= _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated regular expression", start);
          return;
        }
        const code = _base.input.charCodeAt(_base.state.pos);
        if (escaped) {
          escaped = false;
        } else {
          if (code === _charcodes.charCodes.leftSquareBracket) {
            inClass = true;
          } else if (code === _charcodes.charCodes.rightSquareBracket && inClass) {
            inClass = false;
          } else if (code === _charcodes.charCodes.slash && !inClass) {
            break;
          }
          escaped = code === _charcodes.charCodes.backslash;
        }
        ++_base.state.pos;
      }
      ++_base.state.pos;
      skipWord();
      finishToken(_types.TokenType.regexp);
    }
    function readInt() {
      while (true) {
        const code = _base.input.charCodeAt(_base.state.pos);
        if (code >= _charcodes.charCodes.digit0 && code <= _charcodes.charCodes.digit9 || code === _charcodes.charCodes.underscore) {
          _base.state.pos++;
        } else {
          break;
        }
      }
    }
    function readRadixNumber() {
      _base.state.pos += 2;
      while (true) {
        const code = _base.input.charCodeAt(_base.state.pos);
        if (code >= _charcodes.charCodes.digit0 && code <= _charcodes.charCodes.digit9 || code >= _charcodes.charCodes.lowercaseA && code <= _charcodes.charCodes.lowercaseF || code >= _charcodes.charCodes.uppercaseA && code <= _charcodes.charCodes.uppercaseF || code === _charcodes.charCodes.underscore) {
          _base.state.pos++;
        } else {
          break;
        }
      }
      const nextChar = _base.input.charCodeAt(_base.state.pos);
      if (nextChar === _charcodes.charCodes.lowercaseN) {
        ++_base.state.pos;
        finishToken(_types.TokenType.bigint);
      } else {
        finishToken(_types.TokenType.num);
      }
    }
    function readNumber(startsWithDot) {
      let isBigInt = false;
      let isDecimal = false;
      if (!startsWithDot) {
        readInt();
      }
      let nextChar = _base.input.charCodeAt(_base.state.pos);
      if (nextChar === _charcodes.charCodes.dot) {
        ++_base.state.pos;
        readInt();
        nextChar = _base.input.charCodeAt(_base.state.pos);
      }
      if (nextChar === _charcodes.charCodes.uppercaseE || nextChar === _charcodes.charCodes.lowercaseE) {
        nextChar = _base.input.charCodeAt(++_base.state.pos);
        if (nextChar === _charcodes.charCodes.plusSign || nextChar === _charcodes.charCodes.dash) {
          ++_base.state.pos;
        }
        readInt();
        nextChar = _base.input.charCodeAt(_base.state.pos);
      }
      if (nextChar === _charcodes.charCodes.lowercaseN) {
        ++_base.state.pos;
        isBigInt = true;
      } else if (nextChar === _charcodes.charCodes.lowercaseM) {
        ++_base.state.pos;
        isDecimal = true;
      }
      if (isBigInt) {
        finishToken(_types.TokenType.bigint);
        return;
      }
      if (isDecimal) {
        finishToken(_types.TokenType.decimal);
        return;
      }
      finishToken(_types.TokenType.num);
    }
    function readString(quote) {
      _base.state.pos++;
      for (; ; ) {
        if (_base.state.pos >= _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated string constant");
          return;
        }
        const ch = _base.input.charCodeAt(_base.state.pos);
        if (ch === _charcodes.charCodes.backslash) {
          _base.state.pos++;
        } else if (ch === quote) {
          break;
        }
        _base.state.pos++;
      }
      _base.state.pos++;
      finishToken(_types.TokenType.string);
    }
    function readTmplToken() {
      for (; ; ) {
        if (_base.state.pos >= _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated template");
          return;
        }
        const ch = _base.input.charCodeAt(_base.state.pos);
        if (ch === _charcodes.charCodes.graveAccent || ch === _charcodes.charCodes.dollarSign && _base.input.charCodeAt(_base.state.pos + 1) === _charcodes.charCodes.leftCurlyBrace) {
          if (_base.state.pos === _base.state.start && match(_types.TokenType.template)) {
            if (ch === _charcodes.charCodes.dollarSign) {
              _base.state.pos += 2;
              finishToken(_types.TokenType.dollarBraceL);
              return;
            } else {
              ++_base.state.pos;
              finishToken(_types.TokenType.backQuote);
              return;
            }
          }
          finishToken(_types.TokenType.template);
          return;
        }
        if (ch === _charcodes.charCodes.backslash) {
          _base.state.pos++;
        }
        _base.state.pos++;
      }
    }
    function skipWord() {
      while (_base.state.pos < _base.input.length) {
        const ch = _base.input.charCodeAt(_base.state.pos);
        if (_identifier.IS_IDENTIFIER_CHAR[ch]) {
          _base.state.pos++;
        } else if (ch === _charcodes.charCodes.backslash) {
          _base.state.pos += 2;
          if (_base.input.charCodeAt(_base.state.pos) === _charcodes.charCodes.leftCurlyBrace) {
            while (_base.state.pos < _base.input.length && _base.input.charCodeAt(_base.state.pos) !== _charcodes.charCodes.rightCurlyBrace) {
              _base.state.pos++;
            }
            _base.state.pos++;
          }
        } else {
          break;
        }
      }
    }
    exports.skipWord = skipWord;
  }
});

// node_modules/sucrase/dist/util/getImportExportSpecifierInfo.js
var require_getImportExportSpecifierInfo = __commonJS({
  "node_modules/sucrase/dist/util/getImportExportSpecifierInfo.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _types = require_types();
    function getImportExportSpecifierInfo(tokens, index = tokens.currentIndex()) {
      let endIndex = index + 1;
      if (isSpecifierEnd(tokens, endIndex)) {
        const name = tokens.identifierNameAtIndex(index);
        return {
          isType: false,
          leftName: name,
          rightName: name,
          endIndex
        };
      }
      endIndex++;
      if (isSpecifierEnd(tokens, endIndex)) {
        return {
          isType: true,
          leftName: null,
          rightName: null,
          endIndex
        };
      }
      endIndex++;
      if (isSpecifierEnd(tokens, endIndex)) {
        return {
          isType: false,
          leftName: tokens.identifierNameAtIndex(index),
          rightName: tokens.identifierNameAtIndex(index + 2),
          endIndex
        };
      }
      endIndex++;
      if (isSpecifierEnd(tokens, endIndex)) {
        return {
          isType: true,
          leftName: null,
          rightName: null,
          endIndex
        };
      }
      throw new Error(`Unexpected import/export specifier at ${index}`);
    }
    exports.default = getImportExportSpecifierInfo;
    function isSpecifierEnd(tokens, index) {
      const token = tokens.tokens[index];
      return token.type === _types.TokenType.braceR || token.type === _types.TokenType.comma;
    }
  }
});

// node_modules/sucrase/dist/parser/plugins/jsx/xhtml.js
var require_xhtml = __commonJS({
  "node_modules/sucrase/dist/parser/plugins/jsx/xhtml.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.default = /* @__PURE__ */ new Map([
      ["quot", '"'],
      ["amp", "&"],
      ["apos", "'"],
      ["lt", "<"],
      ["gt", ">"],
      ["nbsp", "\xA0"],
      ["iexcl", "\xA1"],
      ["cent", "\xA2"],
      ["pound", "\xA3"],
      ["curren", "\xA4"],
      ["yen", "\xA5"],
      ["brvbar", "\xA6"],
      ["sect", "\xA7"],
      ["uml", "\xA8"],
      ["copy", "\xA9"],
      ["ordf", "\xAA"],
      ["laquo", "\xAB"],
      ["not", "\xAC"],
      ["shy", "\xAD"],
      ["reg", "\xAE"],
      ["macr", "\xAF"],
      ["deg", "\xB0"],
      ["plusmn", "\xB1"],
      ["sup2", "\xB2"],
      ["sup3", "\xB3"],
      ["acute", "\xB4"],
      ["micro", "\xB5"],
      ["para", "\xB6"],
      ["middot", "\xB7"],
      ["cedil", "\xB8"],
      ["sup1", "\xB9"],
      ["ordm", "\xBA"],
      ["raquo", "\xBB"],
      ["frac14", "\xBC"],
      ["frac12", "\xBD"],
      ["frac34", "\xBE"],
      ["iquest", "\xBF"],
      ["Agrave", "\xC0"],
      ["Aacute", "\xC1"],
      ["Acirc", "\xC2"],
      ["Atilde", "\xC3"],
      ["Auml", "\xC4"],
      ["Aring", "\xC5"],
      ["AElig", "\xC6"],
      ["Ccedil", "\xC7"],
      ["Egrave", "\xC8"],
      ["Eacute", "\xC9"],
      ["Ecirc", "\xCA"],
      ["Euml", "\xCB"],
      ["Igrave", "\xCC"],
      ["Iacute", "\xCD"],
      ["Icirc", "\xCE"],
      ["Iuml", "\xCF"],
      ["ETH", "\xD0"],
      ["Ntilde", "\xD1"],
      ["Ograve", "\xD2"],
      ["Oacute", "\xD3"],
      ["Ocirc", "\xD4"],
      ["Otilde", "\xD5"],
      ["Ouml", "\xD6"],
      ["times", "\xD7"],
      ["Oslash", "\xD8"],
      ["Ugrave", "\xD9"],
      ["Uacute", "\xDA"],
      ["Ucirc", "\xDB"],
      ["Uuml", "\xDC"],
      ["Yacute", "\xDD"],
      ["THORN", "\xDE"],
      ["szlig", "\xDF"],
      ["agrave", "\xE0"],
      ["aacute", "\xE1"],
      ["acirc", "\xE2"],
      ["atilde", "\xE3"],
      ["auml", "\xE4"],
      ["aring", "\xE5"],
      ["aelig", "\xE6"],
      ["ccedil", "\xE7"],
      ["egrave", "\xE8"],
      ["eacute", "\xE9"],
      ["ecirc", "\xEA"],
      ["euml", "\xEB"],
      ["igrave", "\xEC"],
      ["iacute", "\xED"],
      ["icirc", "\xEE"],
      ["iuml", "\xEF"],
      ["eth", "\xF0"],
      ["ntilde", "\xF1"],
      ["ograve", "\xF2"],
      ["oacute", "\xF3"],
      ["ocirc", "\xF4"],
      ["otilde", "\xF5"],
      ["ouml", "\xF6"],
      ["divide", "\xF7"],
      ["oslash", "\xF8"],
      ["ugrave", "\xF9"],
      ["uacute", "\xFA"],
      ["ucirc", "\xFB"],
      ["uuml", "\xFC"],
      ["yacute", "\xFD"],
      ["thorn", "\xFE"],
      ["yuml", "\xFF"],
      ["OElig", "\u0152"],
      ["oelig", "\u0153"],
      ["Scaron", "\u0160"],
      ["scaron", "\u0161"],
      ["Yuml", "\u0178"],
      ["fnof", "\u0192"],
      ["circ", "\u02C6"],
      ["tilde", "\u02DC"],
      ["Alpha", "\u0391"],
      ["Beta", "\u0392"],
      ["Gamma", "\u0393"],
      ["Delta", "\u0394"],
      ["Epsilon", "\u0395"],
      ["Zeta", "\u0396"],
      ["Eta", "\u0397"],
      ["Theta", "\u0398"],
      ["Iota", "\u0399"],
      ["Kappa", "\u039A"],
      ["Lambda", "\u039B"],
      ["Mu", "\u039C"],
      ["Nu", "\u039D"],
      ["Xi", "\u039E"],
      ["Omicron", "\u039F"],
      ["Pi", "\u03A0"],
      ["Rho", "\u03A1"],
      ["Sigma", "\u03A3"],
      ["Tau", "\u03A4"],
      ["Upsilon", "\u03A5"],
      ["Phi", "\u03A6"],
      ["Chi", "\u03A7"],
      ["Psi", "\u03A8"],
      ["Omega", "\u03A9"],
      ["alpha", "\u03B1"],
      ["beta", "\u03B2"],
      ["gamma", "\u03B3"],
      ["delta", "\u03B4"],
      ["epsilon", "\u03B5"],
      ["zeta", "\u03B6"],
      ["eta", "\u03B7"],
      ["theta", "\u03B8"],
      ["iota", "\u03B9"],
      ["kappa", "\u03BA"],
      ["lambda", "\u03BB"],
      ["mu", "\u03BC"],
      ["nu", "\u03BD"],
      ["xi", "\u03BE"],
      ["omicron", "\u03BF"],
      ["pi", "\u03C0"],
      ["rho", "\u03C1"],
      ["sigmaf", "\u03C2"],
      ["sigma", "\u03C3"],
      ["tau", "\u03C4"],
      ["upsilon", "\u03C5"],
      ["phi", "\u03C6"],
      ["chi", "\u03C7"],
      ["psi", "\u03C8"],
      ["omega", "\u03C9"],
      ["thetasym", "\u03D1"],
      ["upsih", "\u03D2"],
      ["piv", "\u03D6"],
      ["ensp", "\u2002"],
      ["emsp", "\u2003"],
      ["thinsp", "\u2009"],
      ["zwnj", "\u200C"],
      ["zwj", "\u200D"],
      ["lrm", "\u200E"],
      ["rlm", "\u200F"],
      ["ndash", "\u2013"],
      ["mdash", "\u2014"],
      ["lsquo", "\u2018"],
      ["rsquo", "\u2019"],
      ["sbquo", "\u201A"],
      ["ldquo", "\u201C"],
      ["rdquo", "\u201D"],
      ["bdquo", "\u201E"],
      ["dagger", "\u2020"],
      ["Dagger", "\u2021"],
      ["bull", "\u2022"],
      ["hellip", "\u2026"],
      ["permil", "\u2030"],
      ["prime", "\u2032"],
      ["Prime", "\u2033"],
      ["lsaquo", "\u2039"],
      ["rsaquo", "\u203A"],
      ["oline", "\u203E"],
      ["frasl", "\u2044"],
      ["euro", "\u20AC"],
      ["image", "\u2111"],
      ["weierp", "\u2118"],
      ["real", "\u211C"],
      ["trade", "\u2122"],
      ["alefsym", "\u2135"],
      ["larr", "\u2190"],
      ["uarr", "\u2191"],
      ["rarr", "\u2192"],
      ["darr", "\u2193"],
      ["harr", "\u2194"],
      ["crarr", "\u21B5"],
      ["lArr", "\u21D0"],
      ["uArr", "\u21D1"],
      ["rArr", "\u21D2"],
      ["dArr", "\u21D3"],
      ["hArr", "\u21D4"],
      ["forall", "\u2200"],
      ["part", "\u2202"],
      ["exist", "\u2203"],
      ["empty", "\u2205"],
      ["nabla", "\u2207"],
      ["isin", "\u2208"],
      ["notin", "\u2209"],
      ["ni", "\u220B"],
      ["prod", "\u220F"],
      ["sum", "\u2211"],
      ["minus", "\u2212"],
      ["lowast", "\u2217"],
      ["radic", "\u221A"],
      ["prop", "\u221D"],
      ["infin", "\u221E"],
      ["ang", "\u2220"],
      ["and", "\u2227"],
      ["or", "\u2228"],
      ["cap", "\u2229"],
      ["cup", "\u222A"],
      ["int", "\u222B"],
      ["there4", "\u2234"],
      ["sim", "\u223C"],
      ["cong", "\u2245"],
      ["asymp", "\u2248"],
      ["ne", "\u2260"],
      ["equiv", "\u2261"],
      ["le", "\u2264"],
      ["ge", "\u2265"],
      ["sub", "\u2282"],
      ["sup", "\u2283"],
      ["nsub", "\u2284"],
      ["sube", "\u2286"],
      ["supe", "\u2287"],
      ["oplus", "\u2295"],
      ["otimes", "\u2297"],
      ["perp", "\u22A5"],
      ["sdot", "\u22C5"],
      ["lceil", "\u2308"],
      ["rceil", "\u2309"],
      ["lfloor", "\u230A"],
      ["rfloor", "\u230B"],
      ["lang", "\u2329"],
      ["rang", "\u232A"],
      ["loz", "\u25CA"],
      ["spades", "\u2660"],
      ["clubs", "\u2663"],
      ["hearts", "\u2665"],
      ["diams", "\u2666"]
    ]);
  }
});

// node_modules/sucrase/dist/util/getJSXPragmaInfo.js
var require_getJSXPragmaInfo = __commonJS({
  "node_modules/sucrase/dist/util/getJSXPragmaInfo.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function getJSXPragmaInfo(options) {
      const [base, suffix] = splitPragma(options.jsxPragma || "React.createElement");
      const [fragmentBase, fragmentSuffix] = splitPragma(options.jsxFragmentPragma || "React.Fragment");
      return { base, suffix, fragmentBase, fragmentSuffix };
    }
    exports.default = getJSXPragmaInfo;
    function splitPragma(pragma) {
      let dotIndex = pragma.indexOf(".");
      if (dotIndex === -1) {
        dotIndex = pragma.length;
      }
      return [pragma.slice(0, dotIndex), pragma.slice(dotIndex)];
    }
  }
});

// node_modules/sucrase/dist/transformers/Transformer.js
var require_Transformer = __commonJS({
  "node_modules/sucrase/dist/transformers/Transformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var Transformer = class {
      // Return true if anything was processed, false otherwise.
      getPrefixCode() {
        return "";
      }
      getHoistedCode() {
        return "";
      }
      getSuffixCode() {
        return "";
      }
    };
    exports.default = Transformer;
  }
});

// node_modules/sucrase/dist/transformers/JSXTransformer.js
var require_JSXTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/JSXTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _xhtml = require_xhtml();
    var _xhtml2 = _interopRequireDefault(_xhtml);
    var _tokenizer = require_tokenizer();
    var _types = require_types();
    var _charcodes = require_charcodes();
    var _getJSXPragmaInfo = require_getJSXPragmaInfo();
    var _getJSXPragmaInfo2 = _interopRequireDefault(_getJSXPragmaInfo);
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var JSXTransformer = class _JSXTransformer extends _Transformer2.default {
      // State for calculating the line number of each JSX tag in development.
      __init() {
        this.lastLineNumber = 1;
      }
      __init2() {
        this.lastIndex = 0;
      }
      // In development, variable name holding the name of the current file.
      __init3() {
        this.filenameVarName = null;
      }
      // Mapping of claimed names for imports in the automatic transform, e,g.
      // {jsx: "_jsx"}. This determines which imports to generate in the prefix.
      __init4() {
        this.esmAutomaticImportNameResolutions = {};
      }
      // When automatically adding imports in CJS mode, we store the variable name
      // holding the imported CJS module so we can require it in the prefix.
      __init5() {
        this.cjsAutomaticModuleNameResolutions = {};
      }
      constructor(rootTransformer, tokens, importProcessor, nameManager, options) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.importProcessor = importProcessor;
        this.nameManager = nameManager;
        this.options = options;
        _JSXTransformer.prototype.__init.call(this);
        _JSXTransformer.prototype.__init2.call(this);
        _JSXTransformer.prototype.__init3.call(this);
        _JSXTransformer.prototype.__init4.call(this);
        _JSXTransformer.prototype.__init5.call(this);
        ;
        this.jsxPragmaInfo = _getJSXPragmaInfo2.default.call(void 0, options);
        this.isAutomaticRuntime = options.jsxRuntime === "automatic";
        this.jsxImportSource = options.jsxImportSource || "react";
      }
      process() {
        if (this.tokens.matches1(_types.TokenType.jsxTagStart)) {
          this.processJSXTag();
          return true;
        }
        return false;
      }
      getPrefixCode() {
        let prefix = "";
        if (this.filenameVarName) {
          prefix += `const ${this.filenameVarName} = ${JSON.stringify(this.options.filePath || "")};`;
        }
        if (this.isAutomaticRuntime) {
          if (this.importProcessor) {
            for (const [path, resolvedName] of Object.entries(this.cjsAutomaticModuleNameResolutions)) {
              prefix += `var ${resolvedName} = require("${path}");`;
            }
          } else {
            const { createElement: createElementResolution, ...otherResolutions } = this.esmAutomaticImportNameResolutions;
            if (createElementResolution) {
              prefix += `import {createElement as ${createElementResolution}} from "${this.jsxImportSource}";`;
            }
            const importSpecifiers = Object.entries(otherResolutions).map(([name, resolvedName]) => `${name} as ${resolvedName}`).join(", ");
            if (importSpecifiers) {
              const importPath = this.jsxImportSource + (this.options.production ? "/jsx-runtime" : "/jsx-dev-runtime");
              prefix += `import {${importSpecifiers}} from "${importPath}";`;
            }
          }
        }
        return prefix;
      }
      processJSXTag() {
        const { jsxRole, start } = this.tokens.currentToken();
        const elementLocationCode = this.options.production ? null : this.getElementLocationCode(start);
        if (this.isAutomaticRuntime && jsxRole !== _tokenizer.JSXRole.KeyAfterPropSpread) {
          this.transformTagToJSXFunc(elementLocationCode, jsxRole);
        } else {
          this.transformTagToCreateElement(elementLocationCode);
        }
      }
      getElementLocationCode(firstTokenStart) {
        const lineNumber = this.getLineNumberForIndex(firstTokenStart);
        return `lineNumber: ${lineNumber}`;
      }
      /**
       * Get the line number for this source position. This is calculated lazily and
       * must be called in increasing order by index.
       */
      getLineNumberForIndex(index) {
        const code = this.tokens.code;
        while (this.lastIndex < index && this.lastIndex < code.length) {
          if (code[this.lastIndex] === "\n") {
            this.lastLineNumber++;
          }
          this.lastIndex++;
        }
        return this.lastLineNumber;
      }
      /**
       * Convert the current JSX element to a call to jsx, jsxs, or jsxDEV. This is
       * the primary transformation for the automatic transform.
       *
       * Example:
       * <div a={1} key={2}>Hello{x}</div>
       * becomes
       * jsxs('div', {a: 1, children: ["Hello", x]}, 2)
       */
      transformTagToJSXFunc(elementLocationCode, jsxRole) {
        const isStatic = jsxRole === _tokenizer.JSXRole.StaticChildren;
        this.tokens.replaceToken(this.getJSXFuncInvocationCode(isStatic));
        let keyCode = null;
        if (this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
          this.tokens.replaceToken(`${this.getFragmentCode()}, {`);
          this.processAutomaticChildrenAndEndProps(jsxRole);
        } else {
          this.processTagIntro();
          this.tokens.appendCode(", {");
          keyCode = this.processProps(true);
          if (this.tokens.matches2(_types.TokenType.slash, _types.TokenType.jsxTagEnd)) {
            this.tokens.appendCode("}");
          } else if (this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
            this.tokens.removeToken();
            this.processAutomaticChildrenAndEndProps(jsxRole);
          } else {
            throw new Error("Expected either /> or > at the end of the tag.");
          }
          if (keyCode) {
            this.tokens.appendCode(`, ${keyCode}`);
          }
        }
        if (!this.options.production) {
          if (keyCode === null) {
            this.tokens.appendCode(", void 0");
          }
          this.tokens.appendCode(`, ${isStatic}, ${this.getDevSource(elementLocationCode)}, this`);
        }
        this.tokens.removeInitialToken();
        while (!this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
          this.tokens.removeToken();
        }
        this.tokens.replaceToken(")");
      }
      /**
       * Convert the current JSX element to a createElement call. In the classic
       * runtime, this is the only case. In the automatic runtime, this is called
       * as a fallback in some situations.
       *
       * Example:
       * <div a={1} key={2}>Hello{x}</div>
       * becomes
       * React.createElement('div', {a: 1, key: 2}, "Hello", x)
       */
      transformTagToCreateElement(elementLocationCode) {
        this.tokens.replaceToken(this.getCreateElementInvocationCode());
        if (this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
          this.tokens.replaceToken(`${this.getFragmentCode()}, null`);
          this.processChildren(true);
        } else {
          this.processTagIntro();
          this.processPropsObjectWithDevInfo(elementLocationCode);
          if (this.tokens.matches2(_types.TokenType.slash, _types.TokenType.jsxTagEnd)) {
          } else if (this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
            this.tokens.removeToken();
            this.processChildren(true);
          } else {
            throw new Error("Expected either /> or > at the end of the tag.");
          }
        }
        this.tokens.removeInitialToken();
        while (!this.tokens.matches1(_types.TokenType.jsxTagEnd)) {
          this.tokens.removeToken();
        }
        this.tokens.replaceToken(")");
      }
      /**
       * Get the code for the relevant function for this context: jsx, jsxs,
       * or jsxDEV. The following open-paren is included as well.
       *
       * These functions are only used for the automatic runtime, so they are always
       * auto-imported, but the auto-import will be either CJS or ESM based on the
       * target module format.
       */
      getJSXFuncInvocationCode(isStatic) {
        if (this.options.production) {
          if (isStatic) {
            return this.claimAutoImportedFuncInvocation("jsxs", "/jsx-runtime");
          } else {
            return this.claimAutoImportedFuncInvocation("jsx", "/jsx-runtime");
          }
        } else {
          return this.claimAutoImportedFuncInvocation("jsxDEV", "/jsx-dev-runtime");
        }
      }
      /**
       * Return the code to use for the createElement function, e.g.
       * `React.createElement`, including the following open-paren.
       *
       * This is the main function to use for the classic runtime. For the
       * automatic runtime, this function is used as a fallback function to
       * preserve behavior when there is a prop spread followed by an explicit
       * key. In that automatic runtime case, the function should be automatically
       * imported.
       */
      getCreateElementInvocationCode() {
        if (this.isAutomaticRuntime) {
          return this.claimAutoImportedFuncInvocation("createElement", "");
        } else {
          const { jsxPragmaInfo } = this;
          const resolvedPragmaBaseName = this.importProcessor ? this.importProcessor.getIdentifierReplacement(jsxPragmaInfo.base) || jsxPragmaInfo.base : jsxPragmaInfo.base;
          return `${resolvedPragmaBaseName}${jsxPragmaInfo.suffix}(`;
        }
      }
      /**
       * Return the code to use as the component when compiling a shorthand
       * fragment, e.g. `React.Fragment`.
       *
       * This may be called from either the classic or automatic runtime, and
       * the value should be auto-imported for the automatic runtime.
       */
      getFragmentCode() {
        if (this.isAutomaticRuntime) {
          return this.claimAutoImportedName(
            "Fragment",
            this.options.production ? "/jsx-runtime" : "/jsx-dev-runtime"
          );
        } else {
          const { jsxPragmaInfo } = this;
          const resolvedFragmentPragmaBaseName = this.importProcessor ? this.importProcessor.getIdentifierReplacement(jsxPragmaInfo.fragmentBase) || jsxPragmaInfo.fragmentBase : jsxPragmaInfo.fragmentBase;
          return resolvedFragmentPragmaBaseName + jsxPragmaInfo.fragmentSuffix;
        }
      }
      /**
       * Return code that invokes the given function.
       *
       * When the imports transform is enabled, use the CJSImportTransformer
       * strategy of using `.call(void 0, ...` to avoid passing a `this` value in a
       * situation that would otherwise look like a method call.
       */
      claimAutoImportedFuncInvocation(funcName, importPathSuffix) {
        const funcCode = this.claimAutoImportedName(funcName, importPathSuffix);
        if (this.importProcessor) {
          return `${funcCode}.call(void 0, `;
        } else {
          return `${funcCode}(`;
        }
      }
      claimAutoImportedName(funcName, importPathSuffix) {
        if (this.importProcessor) {
          const path = this.jsxImportSource + importPathSuffix;
          if (!this.cjsAutomaticModuleNameResolutions[path]) {
            this.cjsAutomaticModuleNameResolutions[path] = this.importProcessor.getFreeIdentifierForPath(path);
          }
          return `${this.cjsAutomaticModuleNameResolutions[path]}.${funcName}`;
        } else {
          if (!this.esmAutomaticImportNameResolutions[funcName]) {
            this.esmAutomaticImportNameResolutions[funcName] = this.nameManager.claimFreeName(
              `_${funcName}`
            );
          }
          return this.esmAutomaticImportNameResolutions[funcName];
        }
      }
      /**
       * Process the first part of a tag, before any props.
       */
      processTagIntro() {
        let introEnd = this.tokens.currentIndex() + 1;
        while (this.tokens.tokens[introEnd].isType || !this.tokens.matches2AtIndex(introEnd - 1, _types.TokenType.jsxName, _types.TokenType.jsxName) && !this.tokens.matches2AtIndex(introEnd - 1, _types.TokenType.greaterThan, _types.TokenType.jsxName) && !this.tokens.matches1AtIndex(introEnd, _types.TokenType.braceL) && !this.tokens.matches1AtIndex(introEnd, _types.TokenType.jsxTagEnd) && !this.tokens.matches2AtIndex(introEnd, _types.TokenType.slash, _types.TokenType.jsxTagEnd)) {
          introEnd++;
        }
        if (introEnd === this.tokens.currentIndex() + 1) {
          const tagName = this.tokens.identifierName();
          if (startsWithLowerCase(tagName)) {
            this.tokens.replaceToken(`'${tagName}'`);
          }
        }
        while (this.tokens.currentIndex() < introEnd) {
          this.rootTransformer.processToken();
        }
      }
      /**
       * Starting at the beginning of the props, add the props argument to
       * React.createElement, including the comma before it.
       */
      processPropsObjectWithDevInfo(elementLocationCode) {
        const devProps = this.options.production ? "" : `__self: this, __source: ${this.getDevSource(elementLocationCode)}`;
        if (!this.tokens.matches1(_types.TokenType.jsxName) && !this.tokens.matches1(_types.TokenType.braceL)) {
          if (devProps) {
            this.tokens.appendCode(`, {${devProps}}`);
          } else {
            this.tokens.appendCode(`, null`);
          }
          return;
        }
        this.tokens.appendCode(`, {`);
        this.processProps(false);
        if (devProps) {
          this.tokens.appendCode(` ${devProps}}`);
        } else {
          this.tokens.appendCode("}");
        }
      }
      /**
       * Transform the core part of the props, assuming that a { has already been
       * inserted before us and that a } will be inserted after us.
       *
       * If extractKeyCode is true (i.e. when using any jsx... function), any prop
       * named "key" has its code captured and returned rather than being emitted to
       * the output code. This shifts line numbers, and emitting the code later will
       * correct line numbers again. If no key is found or if extractKeyCode is
       * false, this function returns null.
       */
      processProps(extractKeyCode) {
        let keyCode = null;
        while (true) {
          if (this.tokens.matches2(_types.TokenType.jsxName, _types.TokenType.eq)) {
            const propName = this.tokens.identifierName();
            if (extractKeyCode && propName === "key") {
              if (keyCode !== null) {
                this.tokens.appendCode(keyCode.replace(/[^\n]/g, ""));
              }
              this.tokens.removeToken();
              this.tokens.removeToken();
              const snapshot = this.tokens.snapshot();
              this.processPropValue();
              keyCode = this.tokens.dangerouslyGetAndRemoveCodeSinceSnapshot(snapshot);
              continue;
            } else {
              this.processPropName(propName);
              this.tokens.replaceToken(": ");
              this.processPropValue();
            }
          } else if (this.tokens.matches1(_types.TokenType.jsxName)) {
            const propName = this.tokens.identifierName();
            this.processPropName(propName);
            this.tokens.appendCode(": true");
          } else if (this.tokens.matches1(_types.TokenType.braceL)) {
            this.tokens.replaceToken("");
            this.rootTransformer.processBalancedCode();
            this.tokens.replaceToken("");
          } else {
            break;
          }
          this.tokens.appendCode(",");
        }
        return keyCode;
      }
      processPropName(propName) {
        if (propName.includes("-")) {
          this.tokens.replaceToken(`'${propName}'`);
        } else {
          this.tokens.copyToken();
        }
      }
      processPropValue() {
        if (this.tokens.matches1(_types.TokenType.braceL)) {
          this.tokens.replaceToken("");
          this.rootTransformer.processBalancedCode();
          this.tokens.replaceToken("");
        } else if (this.tokens.matches1(_types.TokenType.jsxTagStart)) {
          this.processJSXTag();
        } else {
          this.processStringPropValue();
        }
      }
      processStringPropValue() {
        const token = this.tokens.currentToken();
        const valueCode = this.tokens.code.slice(token.start + 1, token.end - 1);
        const replacementCode = formatJSXTextReplacement(valueCode);
        const literalCode = formatJSXStringValueLiteral(valueCode);
        this.tokens.replaceToken(literalCode + replacementCode);
      }
      /**
       * Starting in the middle of the props object literal, produce an additional
       * prop for the children and close the object literal.
       */
      processAutomaticChildrenAndEndProps(jsxRole) {
        if (jsxRole === _tokenizer.JSXRole.StaticChildren) {
          this.tokens.appendCode(" children: [");
          this.processChildren(false);
          this.tokens.appendCode("]}");
        } else {
          if (jsxRole === _tokenizer.JSXRole.OneChild) {
            this.tokens.appendCode(" children: ");
          }
          this.processChildren(false);
          this.tokens.appendCode("}");
        }
      }
      /**
       * Transform children into a comma-separated list, which will be either
       * arguments to createElement or array elements of a children prop.
       */
      processChildren(needsInitialComma) {
        let needsComma = needsInitialComma;
        while (true) {
          if (this.tokens.matches2(_types.TokenType.jsxTagStart, _types.TokenType.slash)) {
            return;
          }
          let didEmitElement = false;
          if (this.tokens.matches1(_types.TokenType.braceL)) {
            if (this.tokens.matches2(_types.TokenType.braceL, _types.TokenType.braceR)) {
              this.tokens.replaceToken("");
              this.tokens.replaceToken("");
            } else {
              this.tokens.replaceToken(needsComma ? ", " : "");
              this.rootTransformer.processBalancedCode();
              this.tokens.replaceToken("");
              didEmitElement = true;
            }
          } else if (this.tokens.matches1(_types.TokenType.jsxTagStart)) {
            this.tokens.appendCode(needsComma ? ", " : "");
            this.processJSXTag();
            didEmitElement = true;
          } else if (this.tokens.matches1(_types.TokenType.jsxText) || this.tokens.matches1(_types.TokenType.jsxEmptyText)) {
            didEmitElement = this.processChildTextElement(needsComma);
          } else {
            throw new Error("Unexpected token when processing JSX children.");
          }
          if (didEmitElement) {
            needsComma = true;
          }
        }
      }
      /**
       * Turn a JSX text element into a string literal, or nothing at all if the JSX
       * text resolves to the empty string.
       *
       * Returns true if a string literal is emitted, false otherwise.
       */
      processChildTextElement(needsComma) {
        const token = this.tokens.currentToken();
        const valueCode = this.tokens.code.slice(token.start, token.end);
        const replacementCode = formatJSXTextReplacement(valueCode);
        const literalCode = formatJSXTextLiteral(valueCode);
        if (literalCode === '""') {
          this.tokens.replaceToken(replacementCode);
          return false;
        } else {
          this.tokens.replaceToken(`${needsComma ? ", " : ""}${literalCode}${replacementCode}`);
          return true;
        }
      }
      getDevSource(elementLocationCode) {
        return `{fileName: ${this.getFilenameVarName()}, ${elementLocationCode}}`;
      }
      getFilenameVarName() {
        if (!this.filenameVarName) {
          this.filenameVarName = this.nameManager.claimFreeName("_jsxFileName");
        }
        return this.filenameVarName;
      }
    };
    exports.default = JSXTransformer;
    function startsWithLowerCase(s) {
      const firstChar = s.charCodeAt(0);
      return firstChar >= _charcodes.charCodes.lowercaseA && firstChar <= _charcodes.charCodes.lowercaseZ;
    }
    exports.startsWithLowerCase = startsWithLowerCase;
    function formatJSXTextLiteral(text) {
      let result = "";
      let whitespace = "";
      let isInInitialLineWhitespace = false;
      let seenNonWhitespace = false;
      for (let i = 0; i < text.length; i++) {
        const c = text[i];
        if (c === " " || c === "	" || c === "\r") {
          if (!isInInitialLineWhitespace) {
            whitespace += c;
          }
        } else if (c === "\n") {
          whitespace = "";
          isInInitialLineWhitespace = true;
        } else {
          if (seenNonWhitespace && isInInitialLineWhitespace) {
            result += " ";
          }
          result += whitespace;
          whitespace = "";
          if (c === "&") {
            const { entity, newI } = processEntity(text, i + 1);
            i = newI - 1;
            result += entity;
          } else {
            result += c;
          }
          seenNonWhitespace = true;
          isInInitialLineWhitespace = false;
        }
      }
      if (!isInInitialLineWhitespace) {
        result += whitespace;
      }
      return JSON.stringify(result);
    }
    function formatJSXTextReplacement(text) {
      let numNewlines = 0;
      let numSpaces = 0;
      for (const c of text) {
        if (c === "\n") {
          numNewlines++;
          numSpaces = 0;
        } else if (c === " ") {
          numSpaces++;
        }
      }
      return "\n".repeat(numNewlines) + " ".repeat(numSpaces);
    }
    function formatJSXStringValueLiteral(text) {
      let result = "";
      for (let i = 0; i < text.length; i++) {
        const c = text[i];
        if (c === "\n") {
          if (/\s/.test(text[i + 1])) {
            result += " ";
            while (i < text.length && /\s/.test(text[i + 1])) {
              i++;
            }
          } else {
            result += "\n";
          }
        } else if (c === "&") {
          const { entity, newI } = processEntity(text, i + 1);
          result += entity;
          i = newI - 1;
        } else {
          result += c;
        }
      }
      return JSON.stringify(result);
    }
    function processEntity(text, indexAfterAmpersand) {
      let str = "";
      let count = 0;
      let entity;
      let i = indexAfterAmpersand;
      if (text[i] === "#") {
        let radix = 10;
        i++;
        let numStart;
        if (text[i] === "x") {
          radix = 16;
          i++;
          numStart = i;
          while (i < text.length && isHexDigit2(text.charCodeAt(i))) {
            i++;
          }
        } else {
          numStart = i;
          while (i < text.length && isDecimalDigit2(text.charCodeAt(i))) {
            i++;
          }
        }
        if (text[i] === ";") {
          const numStr = text.slice(numStart, i);
          if (numStr) {
            i++;
            entity = String.fromCodePoint(parseInt(numStr, radix));
          }
        }
      } else {
        while (i < text.length && count++ < 10) {
          const ch = text[i];
          i++;
          if (ch === ";") {
            entity = _xhtml2.default.get(str);
            break;
          }
          str += ch;
        }
      }
      if (!entity) {
        return { entity: "&", newI: indexAfterAmpersand };
      }
      return { entity, newI: i };
    }
    function isDecimalDigit2(code) {
      return code >= _charcodes.charCodes.digit0 && code <= _charcodes.charCodes.digit9;
    }
    function isHexDigit2(code) {
      return code >= _charcodes.charCodes.digit0 && code <= _charcodes.charCodes.digit9 || code >= _charcodes.charCodes.lowercaseA && code <= _charcodes.charCodes.lowercaseF || code >= _charcodes.charCodes.uppercaseA && code <= _charcodes.charCodes.uppercaseF;
    }
  }
});

// node_modules/sucrase/dist/util/getNonTypeIdentifiers.js
var require_getNonTypeIdentifiers = __commonJS({
  "node_modules/sucrase/dist/util/getNonTypeIdentifiers.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tokenizer = require_tokenizer();
    var _types = require_types();
    var _JSXTransformer = require_JSXTransformer();
    var _getJSXPragmaInfo = require_getJSXPragmaInfo();
    var _getJSXPragmaInfo2 = _interopRequireDefault(_getJSXPragmaInfo);
    function getNonTypeIdentifiers(tokens, options) {
      const jsxPragmaInfo = _getJSXPragmaInfo2.default.call(void 0, options);
      const nonTypeIdentifiers = /* @__PURE__ */ new Set();
      for (let i = 0; i < tokens.tokens.length; i++) {
        const token = tokens.tokens[i];
        if (token.type === _types.TokenType.name && !token.isType && (token.identifierRole === _tokenizer.IdentifierRole.Access || token.identifierRole === _tokenizer.IdentifierRole.ObjectShorthand || token.identifierRole === _tokenizer.IdentifierRole.ExportAccess) && !token.shadowsGlobal) {
          nonTypeIdentifiers.add(tokens.identifierNameForToken(token));
        }
        if (token.type === _types.TokenType.jsxTagStart) {
          nonTypeIdentifiers.add(jsxPragmaInfo.base);
        }
        if (token.type === _types.TokenType.jsxTagStart && i + 1 < tokens.tokens.length && tokens.tokens[i + 1].type === _types.TokenType.jsxTagEnd) {
          nonTypeIdentifiers.add(jsxPragmaInfo.base);
          nonTypeIdentifiers.add(jsxPragmaInfo.fragmentBase);
        }
        if (token.type === _types.TokenType.jsxName && token.identifierRole === _tokenizer.IdentifierRole.Access) {
          const identifierName = tokens.identifierNameForToken(token);
          if (!_JSXTransformer.startsWithLowerCase.call(void 0, identifierName) || tokens.tokens[i + 1].type === _types.TokenType.dot) {
            nonTypeIdentifiers.add(tokens.identifierNameForToken(token));
          }
        }
      }
      return nonTypeIdentifiers;
    }
    exports.getNonTypeIdentifiers = getNonTypeIdentifiers;
  }
});

// node_modules/sucrase/dist/CJSImportProcessor.js
var require_CJSImportProcessor = __commonJS({
  "node_modules/sucrase/dist/CJSImportProcessor.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tokenizer = require_tokenizer();
    var _keywords = require_keywords();
    var _types = require_types();
    var _getImportExportSpecifierInfo = require_getImportExportSpecifierInfo();
    var _getImportExportSpecifierInfo2 = _interopRequireDefault(_getImportExportSpecifierInfo);
    var _getNonTypeIdentifiers = require_getNonTypeIdentifiers();
    var CJSImportProcessor = class _CJSImportProcessor {
      __init() {
        this.nonTypeIdentifiers = /* @__PURE__ */ new Set();
      }
      __init2() {
        this.importInfoByPath = /* @__PURE__ */ new Map();
      }
      __init3() {
        this.importsToReplace = /* @__PURE__ */ new Map();
      }
      __init4() {
        this.identifierReplacements = /* @__PURE__ */ new Map();
      }
      __init5() {
        this.exportBindingsByLocalName = /* @__PURE__ */ new Map();
      }
      constructor(nameManager, tokens, enableLegacyTypeScriptModuleInterop, options, isTypeScriptTransformEnabled, keepUnusedImports, helperManager) {
        ;
        this.nameManager = nameManager;
        this.tokens = tokens;
        this.enableLegacyTypeScriptModuleInterop = enableLegacyTypeScriptModuleInterop;
        this.options = options;
        this.isTypeScriptTransformEnabled = isTypeScriptTransformEnabled;
        this.keepUnusedImports = keepUnusedImports;
        this.helperManager = helperManager;
        _CJSImportProcessor.prototype.__init.call(this);
        _CJSImportProcessor.prototype.__init2.call(this);
        _CJSImportProcessor.prototype.__init3.call(this);
        _CJSImportProcessor.prototype.__init4.call(this);
        _CJSImportProcessor.prototype.__init5.call(this);
      }
      preprocessTokens() {
        for (let i = 0; i < this.tokens.tokens.length; i++) {
          if (this.tokens.matches1AtIndex(i, _types.TokenType._import) && !this.tokens.matches3AtIndex(i, _types.TokenType._import, _types.TokenType.name, _types.TokenType.eq)) {
            this.preprocessImportAtIndex(i);
          }
          if (this.tokens.matches1AtIndex(i, _types.TokenType._export) && !this.tokens.matches2AtIndex(i, _types.TokenType._export, _types.TokenType.eq)) {
            this.preprocessExportAtIndex(i);
          }
        }
        this.generateImportReplacements();
      }
      /**
       * In TypeScript, import statements that only import types should be removed.
       * This includes `import {} from 'foo';`, but not `import 'foo';`.
       */
      pruneTypeOnlyImports() {
        this.nonTypeIdentifiers = _getNonTypeIdentifiers.getNonTypeIdentifiers.call(void 0, this.tokens, this.options);
        for (const [path, importInfo] of this.importInfoByPath.entries()) {
          if (importInfo.hasBareImport || importInfo.hasStarExport || importInfo.exportStarNames.length > 0 || importInfo.namedExports.length > 0) {
            continue;
          }
          const names = [
            ...importInfo.defaultNames,
            ...importInfo.wildcardNames,
            ...importInfo.namedImports.map(({ localName }) => localName)
          ];
          if (names.every((name) => this.shouldAutomaticallyElideImportedName(name))) {
            this.importsToReplace.set(path, "");
          }
        }
      }
      shouldAutomaticallyElideImportedName(name) {
        return this.isTypeScriptTransformEnabled && !this.keepUnusedImports && !this.nonTypeIdentifiers.has(name);
      }
      generateImportReplacements() {
        for (const [path, importInfo] of this.importInfoByPath.entries()) {
          const {
            defaultNames,
            wildcardNames,
            namedImports,
            namedExports,
            exportStarNames,
            hasStarExport
          } = importInfo;
          if (defaultNames.length === 0 && wildcardNames.length === 0 && namedImports.length === 0 && namedExports.length === 0 && exportStarNames.length === 0 && !hasStarExport) {
            this.importsToReplace.set(path, `require('${path}');`);
            continue;
          }
          const primaryImportName = this.getFreeIdentifierForPath(path);
          let secondaryImportName;
          if (this.enableLegacyTypeScriptModuleInterop) {
            secondaryImportName = primaryImportName;
          } else {
            secondaryImportName = wildcardNames.length > 0 ? wildcardNames[0] : this.getFreeIdentifierForPath(path);
          }
          let requireCode = `var ${primaryImportName} = require('${path}');`;
          if (wildcardNames.length > 0) {
            for (const wildcardName of wildcardNames) {
              const moduleExpr = this.enableLegacyTypeScriptModuleInterop ? primaryImportName : `${this.helperManager.getHelperName("interopRequireWildcard")}(${primaryImportName})`;
              requireCode += ` var ${wildcardName} = ${moduleExpr};`;
            }
          } else if (exportStarNames.length > 0 && secondaryImportName !== primaryImportName) {
            requireCode += ` var ${secondaryImportName} = ${this.helperManager.getHelperName(
              "interopRequireWildcard"
            )}(${primaryImportName});`;
          } else if (defaultNames.length > 0 && secondaryImportName !== primaryImportName) {
            requireCode += ` var ${secondaryImportName} = ${this.helperManager.getHelperName(
              "interopRequireDefault"
            )}(${primaryImportName});`;
          }
          for (const { importedName, localName } of namedExports) {
            requireCode += ` ${this.helperManager.getHelperName(
              "createNamedExportFrom"
            )}(${primaryImportName}, '${localName}', '${importedName}');`;
          }
          for (const exportStarName of exportStarNames) {
            requireCode += ` exports.${exportStarName} = ${secondaryImportName};`;
          }
          if (hasStarExport) {
            requireCode += ` ${this.helperManager.getHelperName(
              "createStarExport"
            )}(${primaryImportName});`;
          }
          this.importsToReplace.set(path, requireCode);
          for (const defaultName of defaultNames) {
            this.identifierReplacements.set(defaultName, `${secondaryImportName}.default`);
          }
          for (const { importedName, localName } of namedImports) {
            this.identifierReplacements.set(localName, `${primaryImportName}.${importedName}`);
          }
        }
      }
      getFreeIdentifierForPath(path) {
        const components = path.split("/");
        const lastComponent = components[components.length - 1];
        const baseName = lastComponent.replace(/\W/g, "");
        return this.nameManager.claimFreeName(`_${baseName}`);
      }
      preprocessImportAtIndex(index) {
        const defaultNames = [];
        const wildcardNames = [];
        const namedImports = [];
        index++;
        if ((this.tokens.matchesContextualAtIndex(index, _keywords.ContextualKeyword._type) || this.tokens.matches1AtIndex(index, _types.TokenType._typeof)) && !this.tokens.matches1AtIndex(index + 1, _types.TokenType.comma) && !this.tokens.matchesContextualAtIndex(index + 1, _keywords.ContextualKeyword._from)) {
          return;
        }
        if (this.tokens.matches1AtIndex(index, _types.TokenType.parenL)) {
          return;
        }
        if (this.tokens.matches1AtIndex(index, _types.TokenType.name)) {
          defaultNames.push(this.tokens.identifierNameAtIndex(index));
          index++;
          if (this.tokens.matches1AtIndex(index, _types.TokenType.comma)) {
            index++;
          }
        }
        if (this.tokens.matches1AtIndex(index, _types.TokenType.star)) {
          index += 2;
          wildcardNames.push(this.tokens.identifierNameAtIndex(index));
          index++;
        }
        if (this.tokens.matches1AtIndex(index, _types.TokenType.braceL)) {
          const result = this.getNamedImports(index + 1);
          index = result.newIndex;
          for (const namedImport of result.namedImports) {
            if (namedImport.importedName === "default") {
              defaultNames.push(namedImport.localName);
            } else {
              namedImports.push(namedImport);
            }
          }
        }
        if (this.tokens.matchesContextualAtIndex(index, _keywords.ContextualKeyword._from)) {
          index++;
        }
        if (!this.tokens.matches1AtIndex(index, _types.TokenType.string)) {
          throw new Error("Expected string token at the end of import statement.");
        }
        const path = this.tokens.stringValueAtIndex(index);
        const importInfo = this.getImportInfo(path);
        importInfo.defaultNames.push(...defaultNames);
        importInfo.wildcardNames.push(...wildcardNames);
        importInfo.namedImports.push(...namedImports);
        if (defaultNames.length === 0 && wildcardNames.length === 0 && namedImports.length === 0) {
          importInfo.hasBareImport = true;
        }
      }
      preprocessExportAtIndex(index) {
        if (this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType._var) || this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType._let) || this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType._const)) {
          this.preprocessVarExportAtIndex(index);
        } else if (this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType._function) || this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType._class)) {
          const exportName = this.tokens.identifierNameAtIndex(index + 2);
          this.addExportBinding(exportName, exportName);
        } else if (this.tokens.matches3AtIndex(index, _types.TokenType._export, _types.TokenType.name, _types.TokenType._function)) {
          const exportName = this.tokens.identifierNameAtIndex(index + 3);
          this.addExportBinding(exportName, exportName);
        } else if (this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType.braceL)) {
          this.preprocessNamedExportAtIndex(index);
        } else if (this.tokens.matches2AtIndex(index, _types.TokenType._export, _types.TokenType.star)) {
          this.preprocessExportStarAtIndex(index);
        }
      }
      preprocessVarExportAtIndex(index) {
        let depth = 0;
        for (let i = index + 2; ; i++) {
          if (this.tokens.matches1AtIndex(i, _types.TokenType.braceL) || this.tokens.matches1AtIndex(i, _types.TokenType.dollarBraceL) || this.tokens.matches1AtIndex(i, _types.TokenType.bracketL)) {
            depth++;
          } else if (this.tokens.matches1AtIndex(i, _types.TokenType.braceR) || this.tokens.matches1AtIndex(i, _types.TokenType.bracketR)) {
            depth--;
          } else if (depth === 0 && !this.tokens.matches1AtIndex(i, _types.TokenType.name)) {
            break;
          } else if (this.tokens.matches1AtIndex(1, _types.TokenType.eq)) {
            const endIndex = this.tokens.currentToken().rhsEndIndex;
            if (endIndex == null) {
              throw new Error("Expected = token with an end index.");
            }
            i = endIndex - 1;
          } else {
            const token = this.tokens.tokens[i];
            if (_tokenizer.isDeclaration.call(void 0, token)) {
              const exportName = this.tokens.identifierNameAtIndex(i);
              this.identifierReplacements.set(exportName, `exports.${exportName}`);
            }
          }
        }
      }
      /**
       * Walk this export statement just in case it's an export...from statement.
       * If it is, combine it into the import info for that path. Otherwise, just
       * bail out; it'll be handled later.
       */
      preprocessNamedExportAtIndex(index) {
        index += 2;
        const { newIndex, namedImports } = this.getNamedImports(index);
        index = newIndex;
        if (this.tokens.matchesContextualAtIndex(index, _keywords.ContextualKeyword._from)) {
          index++;
        } else {
          for (const { importedName: localName, localName: exportedName } of namedImports) {
            this.addExportBinding(localName, exportedName);
          }
          return;
        }
        if (!this.tokens.matches1AtIndex(index, _types.TokenType.string)) {
          throw new Error("Expected string token at the end of import statement.");
        }
        const path = this.tokens.stringValueAtIndex(index);
        const importInfo = this.getImportInfo(path);
        importInfo.namedExports.push(...namedImports);
      }
      preprocessExportStarAtIndex(index) {
        let exportedName = null;
        if (this.tokens.matches3AtIndex(index, _types.TokenType._export, _types.TokenType.star, _types.TokenType._as)) {
          index += 3;
          exportedName = this.tokens.identifierNameAtIndex(index);
          index += 2;
        } else {
          index += 3;
        }
        if (!this.tokens.matches1AtIndex(index, _types.TokenType.string)) {
          throw new Error("Expected string token at the end of star export statement.");
        }
        const path = this.tokens.stringValueAtIndex(index);
        const importInfo = this.getImportInfo(path);
        if (exportedName !== null) {
          importInfo.exportStarNames.push(exportedName);
        } else {
          importInfo.hasStarExport = true;
        }
      }
      getNamedImports(index) {
        const namedImports = [];
        while (true) {
          if (this.tokens.matches1AtIndex(index, _types.TokenType.braceR)) {
            index++;
            break;
          }
          const specifierInfo = _getImportExportSpecifierInfo2.default.call(void 0, this.tokens, index);
          index = specifierInfo.endIndex;
          if (!specifierInfo.isType) {
            namedImports.push({
              importedName: specifierInfo.leftName,
              localName: specifierInfo.rightName
            });
          }
          if (this.tokens.matches2AtIndex(index, _types.TokenType.comma, _types.TokenType.braceR)) {
            index += 2;
            break;
          } else if (this.tokens.matches1AtIndex(index, _types.TokenType.braceR)) {
            index++;
            break;
          } else if (this.tokens.matches1AtIndex(index, _types.TokenType.comma)) {
            index++;
          } else {
            throw new Error(`Unexpected token: ${JSON.stringify(this.tokens.tokens[index])}`);
          }
        }
        return { newIndex: index, namedImports };
      }
      /**
       * Get a mutable import info object for this path, creating one if it doesn't
       * exist yet.
       */
      getImportInfo(path) {
        const existingInfo = this.importInfoByPath.get(path);
        if (existingInfo) {
          return existingInfo;
        }
        const newInfo = {
          defaultNames: [],
          wildcardNames: [],
          namedImports: [],
          namedExports: [],
          hasBareImport: false,
          exportStarNames: [],
          hasStarExport: false
        };
        this.importInfoByPath.set(path, newInfo);
        return newInfo;
      }
      addExportBinding(localName, exportedName) {
        if (!this.exportBindingsByLocalName.has(localName)) {
          this.exportBindingsByLocalName.set(localName, []);
        }
        this.exportBindingsByLocalName.get(localName).push(exportedName);
      }
      /**
       * Return the code to use for the import for this path, or the empty string if
       * the code has already been "claimed" by a previous import.
       */
      claimImportCode(importPath) {
        const result = this.importsToReplace.get(importPath);
        this.importsToReplace.set(importPath, "");
        return result || "";
      }
      getIdentifierReplacement(identifierName) {
        return this.identifierReplacements.get(identifierName) || null;
      }
      /**
       * Return a string like `exports.foo = exports.bar`.
       */
      resolveExportBinding(assignedName) {
        const exportedNames = this.exportBindingsByLocalName.get(assignedName);
        if (!exportedNames || exportedNames.length === 0) {
          return null;
        }
        return exportedNames.map((exportedName) => `exports.${exportedName}`).join(" = ");
      }
      /**
       * Return all imported/exported names where we might be interested in whether usages of those
       * names are shadowed.
       */
      getGlobalNames() {
        return /* @__PURE__ */ new Set([
          ...this.identifierReplacements.keys(),
          ...this.exportBindingsByLocalName.keys()
        ]);
      }
    };
    exports.default = CJSImportProcessor;
  }
});

// node_modules/@jridgewell/sourcemap-codec/dist/sourcemap-codec.umd.js
var require_sourcemap_codec_umd = __commonJS({
  "node_modules/@jridgewell/sourcemap-codec/dist/sourcemap-codec.umd.js"(exports, module) {
    (function(global, factory) {
      if (typeof exports === "object" && typeof module !== "undefined") {
        factory(module);
        module.exports = def(module);
      } else if (typeof define === "function" && define.amd) {
        define(["module"], function(mod) {
          factory.apply(this, arguments);
          mod.exports = def(mod);
        });
      } else {
        const mod = { exports: {} };
        factory(mod);
        global = typeof globalThis !== "undefined" ? globalThis : global || self;
        global.sourcemapCodec = def(mod);
      }
      function def(m) {
        return "default" in m.exports ? m.exports.default : m.exports;
      }
    })(exports, function(module2) {
      "use strict";
      var __defProp2 = Object.defineProperty;
      var __getOwnPropDesc2 = Object.getOwnPropertyDescriptor;
      var __getOwnPropNames2 = Object.getOwnPropertyNames;
      var __hasOwnProp2 = Object.prototype.hasOwnProperty;
      var __export = (target, all) => {
        for (var name in all)
          __defProp2(target, name, { get: all[name], enumerable: true });
      };
      var __copyProps2 = (to, from, except, desc) => {
        if (from && typeof from === "object" || typeof from === "function") {
          for (let key of __getOwnPropNames2(from))
            if (!__hasOwnProp2.call(to, key) && key !== except)
              __defProp2(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc2(from, key)) || desc.enumerable });
        }
        return to;
      };
      var __toCommonJS = (mod) => __copyProps2(__defProp2({}, "__esModule", { value: true }), mod);
      var sourcemap_codec_exports = {};
      __export(sourcemap_codec_exports, {
        decode: () => decode,
        decodeGeneratedRanges: () => decodeGeneratedRanges,
        decodeOriginalScopes: () => decodeOriginalScopes,
        decodeRangeMappings: () => decodeRangeMappings,
        encode: () => encode,
        encodeGeneratedRanges: () => encodeGeneratedRanges,
        encodeOriginalScopes: () => encodeOriginalScopes,
        encodeRangeMappings: () => encodeRangeMappings
      });
      module2.exports = __toCommonJS(sourcemap_codec_exports);
      var comma = ",".charCodeAt(0);
      var semicolon = ";".charCodeAt(0);
      var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
      var intToChar = new Uint8Array(64);
      var charToInt = new Uint8Array(128);
      for (let i = 0; i < chars.length; i++) {
        const c = chars.charCodeAt(i);
        intToChar[i] = c;
        charToInt[c] = i;
      }
      function decodeInteger(reader) {
        let value = 0;
        let shift = 0;
        let integer = 0;
        do {
          const c = reader.next();
          integer = charToInt[c];
          value |= (integer & 31) << shift;
          shift += 5;
        } while (integer & 32);
        return value;
      }
      function decodeSign(num) {
        return num & 1 ? -2147483648 | -(num >>> 1) : num >>> 1;
      }
      function encodeInteger(builder, num) {
        do {
          let clamped = num & 31;
          num >>>= 5;
          if (num > 0) clamped |= 32;
          builder.write(intToChar[clamped]);
        } while (num > 0);
      }
      function encodeSign(num) {
        return num < 0 ? -num << 1 | 1 : num << 1;
      }
      function hasMoreVlq(reader, max) {
        if (reader.pos >= max) return false;
        return reader.peek() !== comma;
      }
      var bufLength = 1024 * 16;
      var td = typeof TextDecoder !== "undefined" ? /* @__PURE__ */ new TextDecoder() : typeof Buffer !== "undefined" ? {
        decode(buf) {
          const out2 = Buffer.from(buf.buffer, buf.byteOffset, buf.byteLength);
          return out2.toString();
        }
      } : {
        decode(buf) {
          let out2 = "";
          for (let i = 0; i < buf.length; i++) {
            out2 += String.fromCharCode(buf[i]);
          }
          return out2;
        }
      };
      var StringWriter = class {
        constructor() {
          this.pos = 0;
          this.out = "";
          this.buffer = new Uint8Array(bufLength);
        }
        write(v) {
          const { buffer } = this;
          buffer[this.pos++] = v;
          if (this.pos === bufLength) {
            this.out += td.decode(buffer);
            this.pos = 0;
          }
        }
        flush() {
          const { buffer, out: out2, pos } = this;
          return pos > 0 ? out2 + td.decode(buffer.subarray(0, pos)) : out2;
        }
      };
      var StringReader = class {
        constructor(buffer) {
          this.pos = 0;
          this.buffer = buffer;
        }
        next() {
          return this.buffer.charCodeAt(this.pos++);
        }
        peek() {
          return this.buffer.charCodeAt(this.pos);
        }
        indexOf(char) {
          const { buffer, pos } = this;
          const idx = buffer.indexOf(char, pos);
          return idx === -1 ? buffer.length : idx;
        }
      };
      var EMPTY = [];
      function decodeOriginalScopes(input) {
        const { length } = input;
        const reader = new StringReader(input);
        const scopes = [];
        const stack = [];
        let line = 0;
        for (; reader.pos < length; reader.pos++) {
          line += decodeSign(decodeInteger(reader));
          const column = decodeSign(decodeInteger(reader));
          if (!hasMoreVlq(reader, length)) {
            const last = stack.pop();
            last[2] = line;
            last[3] = column;
            continue;
          }
          const kind = decodeSign(decodeInteger(reader));
          const fields = decodeSign(decodeInteger(reader));
          const hasName = fields & 1;
          const scope = hasName ? [line, column, 0, 0, kind, decodeSign(decodeInteger(reader))] : [line, column, 0, 0, kind];
          let vars = EMPTY;
          if (hasMoreVlq(reader, length)) {
            vars = [];
            do {
              const varsIndex = decodeSign(decodeInteger(reader));
              vars.push(varsIndex);
            } while (hasMoreVlq(reader, length));
          }
          scope.vars = vars;
          scopes.push(scope);
          stack.push(scope);
        }
        return scopes;
      }
      function encodeOriginalScopes(scopes) {
        const writer = new StringWriter();
        for (let i = 0; i < scopes.length; ) {
          i = _encodeOriginalScopes(scopes, i, writer, [0]);
        }
        return writer.flush();
      }
      function _encodeOriginalScopes(scopes, index, writer, state) {
        const scope = scopes[index];
        const { 0: startLine, 1: startColumn, 2: endLine, 3: endColumn, 4: kind, vars } = scope;
        if (index > 0) writer.write(comma);
        encodeInteger(writer, encodeSign(startLine - state[0]));
        state[0] = startLine;
        encodeInteger(writer, encodeSign(startColumn));
        encodeInteger(writer, encodeSign(kind));
        const fields = scope.length === 6 ? 1 : 0;
        encodeInteger(writer, encodeSign(fields));
        if (scope.length === 6) encodeInteger(writer, encodeSign(scope[5]));
        for (const v of vars) {
          encodeInteger(writer, encodeSign(v));
        }
        for (index++; index < scopes.length; ) {
          const next = scopes[index];
          const { 0: l, 1: c } = next;
          if (l > endLine || l === endLine && c >= endColumn) {
            break;
          }
          index = _encodeOriginalScopes(scopes, index, writer, state);
        }
        writer.write(comma);
        encodeInteger(writer, encodeSign(endLine - state[0]));
        state[0] = endLine;
        encodeInteger(writer, encodeSign(endColumn));
        return index;
      }
      function decodeGeneratedRanges(input) {
        const { length } = input;
        const reader = new StringReader(input);
        const ranges = [];
        const stack = [];
        let genLine = 0;
        let definitionSourcesIndex = 0;
        let definitionScopeIndex = 0;
        let callsiteSourcesIndex = 0;
        let callsiteLine = 0;
        let callsiteColumn = 0;
        let bindingLine = 0;
        let bindingColumn = 0;
        do {
          const semi = reader.indexOf(";");
          let genColumn = 0;
          for (; reader.pos < semi; reader.pos++) {
            genColumn += decodeSign(decodeInteger(reader));
            if (!hasMoreVlq(reader, semi)) {
              const last = stack.pop();
              last[2] = genLine;
              last[3] = genColumn;
              continue;
            }
            const fields = decodeSign(decodeInteger(reader));
            const hasDefinition = fields & 1;
            const hasCallsite = fields & 2;
            const hasScope = fields & 4;
            let callsite = null;
            let bindings = EMPTY;
            let range;
            if (hasDefinition) {
              const defSourcesIndex = definitionSourcesIndex + decodeSign(decodeInteger(reader));
              definitionScopeIndex = decodeSign(decodeInteger(reader)) + (definitionSourcesIndex === defSourcesIndex ? definitionScopeIndex : 0);
              definitionSourcesIndex = defSourcesIndex;
              range = [genLine, genColumn, 0, 0, defSourcesIndex, definitionScopeIndex];
            } else {
              range = [genLine, genColumn, 0, 0];
            }
            range.isScope = !!hasScope;
            if (hasCallsite) {
              const prevCsi = callsiteSourcesIndex;
              const prevLine = callsiteLine;
              callsiteSourcesIndex += decodeSign(decodeInteger(reader));
              const sameSource = prevCsi === callsiteSourcesIndex;
              callsiteLine = (sameSource ? callsiteLine : 0) + decodeSign(decodeInteger(reader));
              callsiteColumn = (sameSource && prevLine === callsiteLine ? callsiteColumn : 0) + decodeSign(decodeInteger(reader));
              callsite = [callsiteSourcesIndex, callsiteLine, callsiteColumn];
            }
            range.callsite = callsite;
            if (hasMoreVlq(reader, semi)) {
              bindings = [];
              do {
                bindingLine = genLine;
                bindingColumn = genColumn;
                const expressionsCount = decodeSign(decodeInteger(reader));
                let expressionRanges;
                if (expressionsCount < -1) {
                  expressionRanges = [[decodeSign(decodeInteger(reader))]];
                  for (let i = -1; i > expressionsCount; i--) {
                    const prevBl = bindingLine;
                    bindingLine += decodeSign(decodeInteger(reader));
                    bindingColumn = (bindingLine === prevBl ? bindingColumn : 0) + decodeSign(decodeInteger(reader));
                    const expression = decodeSign(decodeInteger(reader));
                    expressionRanges.push([expression, bindingLine, bindingColumn]);
                  }
                } else {
                  expressionRanges = [[expressionsCount]];
                }
                bindings.push(expressionRanges);
              } while (hasMoreVlq(reader, semi));
            }
            range.bindings = bindings;
            ranges.push(range);
            stack.push(range);
          }
          genLine++;
          reader.pos = semi + 1;
        } while (reader.pos < length);
        return ranges;
      }
      function encodeGeneratedRanges(ranges) {
        if (ranges.length === 0) return "";
        const writer = new StringWriter();
        for (let i = 0; i < ranges.length; ) {
          i = _encodeGeneratedRanges(ranges, i, writer, [0, 0, 0, 0, 0, 0, 0]);
        }
        return writer.flush();
      }
      function _encodeGeneratedRanges(ranges, index, writer, state) {
        const range = ranges[index];
        const {
          0: startLine,
          1: startColumn,
          2: endLine,
          3: endColumn,
          isScope,
          callsite,
          bindings
        } = range;
        if (state[0] < startLine) {
          catchupLine(writer, state[0], startLine);
          state[0] = startLine;
          state[1] = 0;
        } else if (index > 0) {
          writer.write(comma);
        }
        encodeInteger(writer, encodeSign(range[1] - state[1]));
        state[1] = range[1];
        const fields = (range.length === 6 ? 1 : 0) | (callsite ? 2 : 0) | (isScope ? 4 : 0);
        encodeInteger(writer, encodeSign(fields));
        if (range.length === 6) {
          const { 4: sourcesIndex, 5: scopesIndex } = range;
          if (sourcesIndex !== state[2]) {
            state[3] = 0;
          }
          encodeInteger(writer, encodeSign(sourcesIndex - state[2]));
          state[2] = sourcesIndex;
          encodeInteger(writer, encodeSign(scopesIndex - state[3]));
          state[3] = scopesIndex;
        }
        if (callsite) {
          const { 0: sourcesIndex, 1: callLine, 2: callColumn } = range.callsite;
          if (sourcesIndex !== state[4]) {
            state[5] = 0;
            state[6] = 0;
          } else if (callLine !== state[5]) {
            state[6] = 0;
          }
          encodeInteger(writer, encodeSign(sourcesIndex - state[4]));
          state[4] = sourcesIndex;
          encodeInteger(writer, encodeSign(callLine - state[5]));
          state[5] = callLine;
          encodeInteger(writer, encodeSign(callColumn - state[6]));
          state[6] = callColumn;
        }
        if (bindings) {
          for (const binding of bindings) {
            if (binding.length > 1) encodeInteger(writer, encodeSign(-binding.length));
            const expression = binding[0][0];
            encodeInteger(writer, encodeSign(expression));
            let bindingStartLine = startLine;
            let bindingStartColumn = startColumn;
            for (let i = 1; i < binding.length; i++) {
              const expRange = binding[i];
              encodeInteger(writer, encodeSign(expRange[1] - bindingStartLine));
              bindingStartLine = expRange[1];
              encodeInteger(writer, encodeSign(expRange[2] - bindingStartColumn));
              bindingStartColumn = expRange[2];
              encodeInteger(writer, encodeSign(expRange[0]));
            }
          }
        }
        for (index++; index < ranges.length; ) {
          const next = ranges[index];
          const { 0: l, 1: c } = next;
          if (l > endLine || l === endLine && c >= endColumn) {
            break;
          }
          index = _encodeGeneratedRanges(ranges, index, writer, state);
        }
        if (state[0] < endLine) {
          catchupLine(writer, state[0], endLine);
          state[0] = endLine;
          state[1] = 0;
        } else {
          writer.write(comma);
        }
        encodeInteger(writer, encodeSign(endColumn - state[1]));
        state[1] = endColumn;
        return index;
      }
      function catchupLine(writer, lastLine, line) {
        do {
          writer.write(semicolon);
        } while (++lastLine < line);
      }
      function decodeRangeMappings(input) {
        const { length } = input;
        const reader = new StringReader(input);
        const rangeMappings = [];
        do {
          const semi = reader.indexOf(";");
          const indices = [];
          let index = 0;
          while (reader.pos < semi) {
            index += decodeInteger(reader);
            indices.push(index);
          }
          rangeMappings.push(indices);
          reader.pos = semi + 1;
        } while (reader.pos <= length);
        return rangeMappings;
      }
      function encodeRangeMappings(decoded) {
        if (decoded.length === 0) return "";
        const writer = new StringWriter();
        for (let i = 0; i < decoded.length; i++) {
          const indices = decoded[i];
          if (i > 0) writer.write(semicolon);
          let index = 0;
          for (let j = 0; j < indices.length; j++) {
            const offset2 = indices[j];
            encodeInteger(writer, offset2 - index);
            index = offset2;
          }
        }
        return writer.flush();
      }
      function decode(mappings) {
        const { length } = mappings;
        const reader = new StringReader(mappings);
        const decoded = [];
        let genColumn = 0;
        let sourcesIndex = 0;
        let sourceLine = 0;
        let sourceColumn = 0;
        let namesIndex = 0;
        do {
          const semi = reader.indexOf(";");
          const line = [];
          let sorted = true;
          let lastCol = 0;
          genColumn = 0;
          while (reader.pos < semi) {
            let seg;
            genColumn += decodeSign(decodeInteger(reader));
            if (genColumn < lastCol) sorted = false;
            lastCol = genColumn;
            if (hasMoreVlq(reader, semi)) {
              sourcesIndex += decodeSign(decodeInteger(reader));
              sourceLine += decodeSign(decodeInteger(reader));
              sourceColumn += decodeSign(decodeInteger(reader));
              if (hasMoreVlq(reader, semi)) {
                namesIndex += decodeSign(decodeInteger(reader));
                seg = [genColumn, sourcesIndex, sourceLine, sourceColumn, namesIndex];
              } else {
                seg = [genColumn, sourcesIndex, sourceLine, sourceColumn];
              }
            } else {
              seg = [genColumn];
            }
            line.push(seg);
            reader.pos++;
          }
          if (!sorted) sort(line);
          decoded.push(line);
          reader.pos = semi + 1;
        } while (reader.pos <= length);
        return decoded;
      }
      function sort(line) {
        line.sort(sortComparator);
      }
      function sortComparator(a, b) {
        return a[0] - b[0];
      }
      function encode(decoded) {
        const writer = new StringWriter();
        let sourcesIndex = 0;
        let sourceLine = 0;
        let sourceColumn = 0;
        let namesIndex = 0;
        for (let i = 0; i < decoded.length; i++) {
          const line = decoded[i];
          if (i > 0) writer.write(semicolon);
          if (line.length === 0) continue;
          let genColumn = 0;
          for (let j = 0; j < line.length; j++) {
            const segment = line[j];
            if (j > 0) writer.write(comma);
            encodeInteger(writer, encodeSign(segment[0] - genColumn));
            genColumn = segment[0];
            if (segment.length === 1) continue;
            encodeInteger(writer, encodeSign(segment[1] - sourcesIndex));
            encodeInteger(writer, encodeSign(segment[2] - sourceLine));
            encodeInteger(writer, encodeSign(segment[3] - sourceColumn));
            sourcesIndex = segment[1];
            sourceLine = segment[2];
            sourceColumn = segment[3];
            if (segment.length === 4) continue;
            encodeInteger(writer, encodeSign(segment[4] - namesIndex));
            namesIndex = segment[4];
          }
        }
        return writer.flush();
      }
    });
  }
});

// node_modules/@jridgewell/resolve-uri/dist/resolve-uri.umd.js
var require_resolve_uri_umd = __commonJS({
  "node_modules/@jridgewell/resolve-uri/dist/resolve-uri.umd.js"(exports, module) {
    (function(global, factory) {
      typeof exports === "object" && typeof module !== "undefined" ? module.exports = factory() : typeof define === "function" && define.amd ? define(factory) : (global = typeof globalThis !== "undefined" ? globalThis : global || self, global.resolveURI = factory());
    })(exports, function() {
      "use strict";
      const schemeRegex = /^[\w+.-]+:\/\//;
      const urlRegex = /^([\w+.-]+:)\/\/([^@/#?]*@)?([^:/#?]*)(:\d+)?(\/[^#?]*)?(\?[^#]*)?(#.*)?/;
      const fileRegex = /^file:(?:\/\/((?![a-z]:)[^/#?]*)?)?(\/?[^#?]*)(\?[^#]*)?(#.*)?/i;
      function isAbsoluteUrl(input) {
        return schemeRegex.test(input);
      }
      function isSchemeRelativeUrl(input) {
        return input.startsWith("//");
      }
      function isAbsolutePath(input) {
        return input.startsWith("/");
      }
      function isFileUrl(input) {
        return input.startsWith("file:");
      }
      function isRelative(input) {
        return /^[.?#]/.test(input);
      }
      function parseAbsoluteUrl(input) {
        const match = urlRegex.exec(input);
        return makeUrl(match[1], match[2] || "", match[3], match[4] || "", match[5] || "/", match[6] || "", match[7] || "");
      }
      function parseFileUrl(input) {
        const match = fileRegex.exec(input);
        const path = match[2];
        return makeUrl("file:", "", match[1] || "", "", isAbsolutePath(path) ? path : "/" + path, match[3] || "", match[4] || "");
      }
      function makeUrl(scheme, user, host, port, path, query, hash) {
        return {
          scheme,
          user,
          host,
          port,
          path,
          query,
          hash,
          type: 7
        };
      }
      function parseUrl(input) {
        if (isSchemeRelativeUrl(input)) {
          const url2 = parseAbsoluteUrl("http:" + input);
          url2.scheme = "";
          url2.type = 6;
          return url2;
        }
        if (isAbsolutePath(input)) {
          const url2 = parseAbsoluteUrl("http://foo.com" + input);
          url2.scheme = "";
          url2.host = "";
          url2.type = 5;
          return url2;
        }
        if (isFileUrl(input))
          return parseFileUrl(input);
        if (isAbsoluteUrl(input))
          return parseAbsoluteUrl(input);
        const url = parseAbsoluteUrl("http://foo.com/" + input);
        url.scheme = "";
        url.host = "";
        url.type = input ? input.startsWith("?") ? 3 : input.startsWith("#") ? 2 : 4 : 1;
        return url;
      }
      function stripPathFilename(path) {
        if (path.endsWith("/.."))
          return path;
        const index = path.lastIndexOf("/");
        return path.slice(0, index + 1);
      }
      function mergePaths(url, base) {
        normalizePath(base, base.type);
        if (url.path === "/") {
          url.path = base.path;
        } else {
          url.path = stripPathFilename(base.path) + url.path;
        }
      }
      function normalizePath(url, type) {
        const rel = type <= 4;
        const pieces = url.path.split("/");
        let pointer = 1;
        let positive = 0;
        let addTrailingSlash = false;
        for (let i = 1; i < pieces.length; i++) {
          const piece = pieces[i];
          if (!piece) {
            addTrailingSlash = true;
            continue;
          }
          addTrailingSlash = false;
          if (piece === ".")
            continue;
          if (piece === "..") {
            if (positive) {
              addTrailingSlash = true;
              positive--;
              pointer--;
            } else if (rel) {
              pieces[pointer++] = piece;
            }
            continue;
          }
          pieces[pointer++] = piece;
          positive++;
        }
        let path = "";
        for (let i = 1; i < pointer; i++) {
          path += "/" + pieces[i];
        }
        if (!path || addTrailingSlash && !path.endsWith("/..")) {
          path += "/";
        }
        url.path = path;
      }
      function resolve(input, base) {
        if (!input && !base)
          return "";
        const url = parseUrl(input);
        let inputType = url.type;
        if (base && inputType !== 7) {
          const baseUrl = parseUrl(base);
          const baseType = baseUrl.type;
          switch (inputType) {
            case 1:
              url.hash = baseUrl.hash;
            // fall through
            case 2:
              url.query = baseUrl.query;
            // fall through
            case 3:
            case 4:
              mergePaths(url, baseUrl);
            // fall through
            case 5:
              url.user = baseUrl.user;
              url.host = baseUrl.host;
              url.port = baseUrl.port;
            // fall through
            case 6:
              url.scheme = baseUrl.scheme;
          }
          if (baseType > inputType)
            inputType = baseType;
        }
        normalizePath(url, inputType);
        const queryHash = url.query + url.hash;
        switch (inputType) {
          // This is impossible, because of the empty checks at the start of the function.
          // case UrlType.Empty:
          case 2:
          case 3:
            return queryHash;
          case 4: {
            const path = url.path.slice(1);
            if (!path)
              return queryHash || ".";
            if (isRelative(base || input) && !isRelative(path)) {
              return "./" + path + queryHash;
            }
            return path + queryHash;
          }
          case 5:
            return url.path + queryHash;
          default:
            return url.scheme + "//" + url.user + url.host + url.port + url.path + queryHash;
        }
      }
      return resolve;
    });
  }
});

// node_modules/@jridgewell/trace-mapping/dist/trace-mapping.umd.js
var require_trace_mapping_umd = __commonJS({
  "node_modules/@jridgewell/trace-mapping/dist/trace-mapping.umd.js"(exports, module) {
    (function(global, factory) {
      if (typeof exports === "object" && typeof module !== "undefined") {
        factory(module, require_resolve_uri_umd(), require_sourcemap_codec_umd());
        module.exports = def(module);
      } else if (typeof define === "function" && define.amd) {
        define(["module", "@jridgewell/resolve-uri", "@jridgewell/sourcemap-codec"], function(mod) {
          factory.apply(this, arguments);
          mod.exports = def(mod);
        });
      } else {
        const mod = { exports: {} };
        factory(mod, global.resolveURI, global.sourcemapCodec);
        global = typeof globalThis !== "undefined" ? globalThis : global || self;
        global.traceMapping = def(mod);
      }
      function def(m) {
        return "default" in m.exports ? m.exports.default : m.exports;
      }
    })(exports, function(module2, require_resolveURI, require_sourcemapCodec) {
      "use strict";
      var __create2 = Object.create;
      var __defProp2 = Object.defineProperty;
      var __getOwnPropDesc2 = Object.getOwnPropertyDescriptor;
      var __getOwnPropNames2 = Object.getOwnPropertyNames;
      var __getProtoOf2 = Object.getPrototypeOf;
      var __hasOwnProp2 = Object.prototype.hasOwnProperty;
      var __commonJS2 = (cb, mod) => function __require() {
        return mod || (0, cb[__getOwnPropNames2(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
      };
      var __export = (target, all) => {
        for (var name in all)
          __defProp2(target, name, { get: all[name], enumerable: true });
      };
      var __copyProps2 = (to, from, except, desc) => {
        if (from && typeof from === "object" || typeof from === "function") {
          for (let key of __getOwnPropNames2(from))
            if (!__hasOwnProp2.call(to, key) && key !== except)
              __defProp2(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc2(from, key)) || desc.enumerable });
        }
        return to;
      };
      var __toESM2 = (mod, isNodeMode, target) => (target = mod != null ? __create2(__getProtoOf2(mod)) : {}, __copyProps2(
        // If the importer is in node compatibility mode or this is not an ESM
        // file that has been converted to a CommonJS file using a Babel-
        // compatible transform (i.e. "__esModule" has not been set), then set
        // "default" to the CommonJS "module.exports" for node compatibility.
        isNodeMode || !mod || !mod.__esModule ? __defProp2(target, "default", { value: mod, enumerable: true }) : target,
        mod
      ));
      var __toCommonJS = (mod) => __copyProps2(__defProp2({}, "__esModule", { value: true }), mod);
      var require_sourcemap_codec = __commonJS2({
        "umd:@jridgewell/sourcemap-codec"(exports2, module22) {
          module22.exports = require_sourcemapCodec;
        }
      });
      var require_resolve_uri = __commonJS2({
        "umd:@jridgewell/resolve-uri"(exports2, module22) {
          module22.exports = require_resolveURI;
        }
      });
      var trace_mapping_exports = {};
      __export(trace_mapping_exports, {
        AnyMap: () => FlattenMap,
        FlattenMap: () => FlattenMap,
        GREATEST_LOWER_BOUND: () => GREATEST_LOWER_BOUND,
        LEAST_UPPER_BOUND: () => LEAST_UPPER_BOUND,
        TraceMap: () => TraceMap,
        allGeneratedPositionsFor: () => allGeneratedPositionsFor,
        decodedMap: () => decodedMap,
        decodedMappings: () => decodedMappings,
        eachMapping: () => eachMapping,
        encodedMap: () => encodedMap,
        encodedMappings: () => encodedMappings,
        generatedPositionFor: () => generatedPositionFor,
        isIgnored: () => isIgnored,
        originalPositionFor: () => originalPositionFor,
        presortedDecodedMap: () => presortedDecodedMap,
        sourceContentFor: () => sourceContentFor,
        traceSegment: () => traceSegment
      });
      module2.exports = __toCommonJS(trace_mapping_exports);
      var import_sourcemap_codec = __toESM2(require_sourcemap_codec());
      var import_resolve_uri = __toESM2(require_resolve_uri());
      function stripFilename(path) {
        if (!path) return "";
        const index = path.lastIndexOf("/");
        return path.slice(0, index + 1);
      }
      function resolver(mapUrl, sourceRoot) {
        const from = stripFilename(mapUrl);
        const prefix = sourceRoot ? sourceRoot + "/" : "";
        return (source) => (0, import_resolve_uri.default)(prefix + (source || ""), from);
      }
      var COLUMN = 0;
      var SOURCES_INDEX = 1;
      var SOURCE_LINE = 2;
      var SOURCE_COLUMN = 3;
      var NAMES_INDEX = 4;
      var REV_GENERATED_LINE = 1;
      var REV_GENERATED_COLUMN = 2;
      function maybeSort(mappings, owned) {
        const unsortedIndex = nextUnsortedSegmentLine(mappings, 0);
        if (unsortedIndex === mappings.length) return mappings;
        if (!owned) mappings = mappings.slice();
        for (let i = unsortedIndex; i < mappings.length; i = nextUnsortedSegmentLine(mappings, i + 1)) {
          mappings[i] = sortSegments(mappings[i], owned);
        }
        return mappings;
      }
      function nextUnsortedSegmentLine(mappings, start) {
        for (let i = start; i < mappings.length; i++) {
          if (!isSorted(mappings[i])) return i;
        }
        return mappings.length;
      }
      function isSorted(line) {
        for (let j = 1; j < line.length; j++) {
          if (line[j][COLUMN] < line[j - 1][COLUMN]) {
            return false;
          }
        }
        return true;
      }
      function sortSegments(line, owned) {
        if (!owned) line = line.slice();
        return line.sort(sortComparator);
      }
      function sortComparator(a, b) {
        return a[COLUMN] - b[COLUMN];
      }
      function buildBySources(decoded, memos) {
        const sources = memos.map(() => []);
        for (let i = 0; i < decoded.length; i++) {
          const line = decoded[i];
          for (let j = 0; j < line.length; j++) {
            const seg = line[j];
            if (seg.length === 1) continue;
            const sourceIndex2 = seg[SOURCES_INDEX];
            const sourceLine = seg[SOURCE_LINE];
            const sourceColumn = seg[SOURCE_COLUMN];
            const source = sources[sourceIndex2];
            const segs = source[sourceLine] || (source[sourceLine] = []);
            segs.push([sourceColumn, i, seg[COLUMN]]);
          }
        }
        for (let i = 0; i < sources.length; i++) {
          const source = sources[i];
          for (let j = 0; j < source.length; j++) {
            const line = source[j];
            if (line) line.sort(sortComparator);
          }
        }
        return sources;
      }
      var found = false;
      function binarySearch(haystack, needle, low, high) {
        while (low <= high) {
          const mid = low + (high - low >> 1);
          const cmp = haystack[mid][COLUMN] - needle;
          if (cmp === 0) {
            found = true;
            return mid;
          }
          if (cmp < 0) {
            low = mid + 1;
          } else {
            high = mid - 1;
          }
        }
        found = false;
        return low - 1;
      }
      function upperBound(haystack, needle, index) {
        for (let i = index + 1; i < haystack.length; index = i++) {
          if (haystack[i][COLUMN] !== needle) break;
        }
        return index;
      }
      function lowerBound(haystack, needle, index) {
        for (let i = index - 1; i >= 0; index = i--) {
          if (haystack[i][COLUMN] !== needle) break;
        }
        return index;
      }
      function memoizedState() {
        return {
          lastKey: -1,
          lastNeedle: -1,
          lastIndex: -1
        };
      }
      function memoizedBinarySearch(haystack, needle, state, key) {
        const { lastKey, lastNeedle, lastIndex } = state;
        let low = 0;
        let high = haystack.length - 1;
        if (key === lastKey) {
          if (needle === lastNeedle) {
            found = lastIndex !== -1 && haystack[lastIndex][COLUMN] === needle;
            return lastIndex;
          }
          if (needle >= lastNeedle) {
            low = lastIndex === -1 ? 0 : lastIndex;
          } else {
            high = lastIndex;
          }
        }
        state.lastKey = key;
        state.lastNeedle = needle;
        return state.lastIndex = binarySearch(haystack, needle, low, high);
      }
      function parse4(map) {
        return typeof map === "string" ? JSON.parse(map) : map;
      }
      var FlattenMap = function(map, mapUrl) {
        const parsed = parse4(map);
        if (!("sections" in parsed)) {
          return new TraceMap(parsed, mapUrl);
        }
        const mappings = [];
        const sources = [];
        const sourcesContent = [];
        const names = [];
        const ignoreList = [];
        recurse(
          parsed,
          mapUrl,
          mappings,
          sources,
          sourcesContent,
          names,
          ignoreList,
          0,
          0,
          Infinity,
          Infinity
        );
        const joined = {
          version: 3,
          file: parsed.file,
          names,
          sources,
          sourcesContent,
          mappings,
          ignoreList
        };
        return presortedDecodedMap(joined);
      };
      function recurse(input, mapUrl, mappings, sources, sourcesContent, names, ignoreList, lineOffset, columnOffset, stopLine, stopColumn) {
        const { sections } = input;
        for (let i = 0; i < sections.length; i++) {
          const { map, offset: offset2 } = sections[i];
          let sl = stopLine;
          let sc = stopColumn;
          if (i + 1 < sections.length) {
            const nextOffset = sections[i + 1].offset;
            sl = Math.min(stopLine, lineOffset + nextOffset.line);
            if (sl === stopLine) {
              sc = Math.min(stopColumn, columnOffset + nextOffset.column);
            } else if (sl < stopLine) {
              sc = columnOffset + nextOffset.column;
            }
          }
          addSection(
            map,
            mapUrl,
            mappings,
            sources,
            sourcesContent,
            names,
            ignoreList,
            lineOffset + offset2.line,
            columnOffset + offset2.column,
            sl,
            sc
          );
        }
      }
      function addSection(input, mapUrl, mappings, sources, sourcesContent, names, ignoreList, lineOffset, columnOffset, stopLine, stopColumn) {
        const parsed = parse4(input);
        if ("sections" in parsed) return recurse(...arguments);
        const map = new TraceMap(parsed, mapUrl);
        const sourcesOffset = sources.length;
        const namesOffset = names.length;
        const decoded = decodedMappings(map);
        const { resolvedSources, sourcesContent: contents, ignoreList: ignores } = map;
        append(sources, resolvedSources);
        append(names, map.names);
        if (contents) append(sourcesContent, contents);
        else for (let i = 0; i < resolvedSources.length; i++) sourcesContent.push(null);
        if (ignores) for (let i = 0; i < ignores.length; i++) ignoreList.push(ignores[i] + sourcesOffset);
        for (let i = 0; i < decoded.length; i++) {
          const lineI = lineOffset + i;
          if (lineI > stopLine) return;
          const out2 = getLine(mappings, lineI);
          const cOffset = i === 0 ? columnOffset : 0;
          const line = decoded[i];
          for (let j = 0; j < line.length; j++) {
            const seg = line[j];
            const column = cOffset + seg[COLUMN];
            if (lineI === stopLine && column >= stopColumn) return;
            if (seg.length === 1) {
              out2.push([column]);
              continue;
            }
            const sourcesIndex = sourcesOffset + seg[SOURCES_INDEX];
            const sourceLine = seg[SOURCE_LINE];
            const sourceColumn = seg[SOURCE_COLUMN];
            out2.push(
              seg.length === 4 ? [column, sourcesIndex, sourceLine, sourceColumn] : [column, sourcesIndex, sourceLine, sourceColumn, namesOffset + seg[NAMES_INDEX]]
            );
          }
        }
      }
      function append(arr, other) {
        for (let i = 0; i < other.length; i++) arr.push(other[i]);
      }
      function getLine(arr, index) {
        for (let i = arr.length; i <= index; i++) arr[i] = [];
        return arr[index];
      }
      var LINE_GTR_ZERO = "`line` must be greater than 0 (lines start at line 1)";
      var COL_GTR_EQ_ZERO = "`column` must be greater than or equal to 0 (columns start at column 0)";
      var LEAST_UPPER_BOUND = -1;
      var GREATEST_LOWER_BOUND = 1;
      var TraceMap = class {
        constructor(map, mapUrl) {
          const isString = typeof map === "string";
          if (!isString && map._decodedMemo) return map;
          const parsed = parse4(map);
          const { version: version2, file: file2, names, sourceRoot, sources, sourcesContent } = parsed;
          this.version = version2;
          this.file = file2;
          this.names = names || [];
          this.sourceRoot = sourceRoot;
          this.sources = sources;
          this.sourcesContent = sourcesContent;
          this.ignoreList = parsed.ignoreList || parsed.x_google_ignoreList || void 0;
          const resolve = resolver(mapUrl, sourceRoot);
          this.resolvedSources = sources.map(resolve);
          const { mappings } = parsed;
          if (typeof mappings === "string") {
            this._encoded = mappings;
            this._decoded = void 0;
          } else if (Array.isArray(mappings)) {
            this._encoded = void 0;
            this._decoded = maybeSort(mappings, isString);
          } else if (parsed.sections) {
            throw new Error(`TraceMap passed sectioned source map, please use FlattenMap export instead`);
          } else {
            throw new Error(`invalid source map: ${JSON.stringify(parsed)}`);
          }
          this._decodedMemo = memoizedState();
          this._bySources = void 0;
          this._bySourceMemos = void 0;
        }
      };
      function cast(map) {
        return map;
      }
      function encodedMappings(map) {
        var _a, _b;
        return (_b = (_a = cast(map))._encoded) != null ? _b : _a._encoded = (0, import_sourcemap_codec.encode)(cast(map)._decoded);
      }
      function decodedMappings(map) {
        var _a;
        return (_a = cast(map))._decoded || (_a._decoded = (0, import_sourcemap_codec.decode)(cast(map)._encoded));
      }
      function traceSegment(map, line, column) {
        const decoded = decodedMappings(map);
        if (line >= decoded.length) return null;
        const segments = decoded[line];
        const index = traceSegmentInternal(
          segments,
          cast(map)._decodedMemo,
          line,
          column,
          GREATEST_LOWER_BOUND
        );
        return index === -1 ? null : segments[index];
      }
      function originalPositionFor(map, needle) {
        let { line, column, bias } = needle;
        line--;
        if (line < 0) throw new Error(LINE_GTR_ZERO);
        if (column < 0) throw new Error(COL_GTR_EQ_ZERO);
        const decoded = decodedMappings(map);
        if (line >= decoded.length) return OMapping(null, null, null, null);
        const segments = decoded[line];
        const index = traceSegmentInternal(
          segments,
          cast(map)._decodedMemo,
          line,
          column,
          bias || GREATEST_LOWER_BOUND
        );
        if (index === -1) return OMapping(null, null, null, null);
        const segment = segments[index];
        if (segment.length === 1) return OMapping(null, null, null, null);
        const { names, resolvedSources } = map;
        return OMapping(
          resolvedSources[segment[SOURCES_INDEX]],
          segment[SOURCE_LINE] + 1,
          segment[SOURCE_COLUMN],
          segment.length === 5 ? names[segment[NAMES_INDEX]] : null
        );
      }
      function generatedPositionFor(map, needle) {
        const { source, line, column, bias } = needle;
        return generatedPosition(map, source, line, column, bias || GREATEST_LOWER_BOUND, false);
      }
      function allGeneratedPositionsFor(map, needle) {
        const { source, line, column, bias } = needle;
        return generatedPosition(map, source, line, column, bias || LEAST_UPPER_BOUND, true);
      }
      function eachMapping(map, cb) {
        const decoded = decodedMappings(map);
        const { names, resolvedSources } = map;
        for (let i = 0; i < decoded.length; i++) {
          const line = decoded[i];
          for (let j = 0; j < line.length; j++) {
            const seg = line[j];
            const generatedLine = i + 1;
            const generatedColumn = seg[0];
            let source = null;
            let originalLine = null;
            let originalColumn = null;
            let name = null;
            if (seg.length !== 1) {
              source = resolvedSources[seg[1]];
              originalLine = seg[2] + 1;
              originalColumn = seg[3];
            }
            if (seg.length === 5) name = names[seg[4]];
            cb({
              generatedLine,
              generatedColumn,
              source,
              originalLine,
              originalColumn,
              name
            });
          }
        }
      }
      function sourceIndex(map, source) {
        const { sources, resolvedSources } = map;
        let index = sources.indexOf(source);
        if (index === -1) index = resolvedSources.indexOf(source);
        return index;
      }
      function sourceContentFor(map, source) {
        const { sourcesContent } = map;
        if (sourcesContent == null) return null;
        const index = sourceIndex(map, source);
        return index === -1 ? null : sourcesContent[index];
      }
      function isIgnored(map, source) {
        const { ignoreList } = map;
        if (ignoreList == null) return false;
        const index = sourceIndex(map, source);
        return index === -1 ? false : ignoreList.includes(index);
      }
      function presortedDecodedMap(map, mapUrl) {
        const tracer = new TraceMap(clone(map, []), mapUrl);
        cast(tracer)._decoded = map.mappings;
        return tracer;
      }
      function decodedMap(map) {
        return clone(map, decodedMappings(map));
      }
      function encodedMap(map) {
        return clone(map, encodedMappings(map));
      }
      function clone(map, mappings) {
        return {
          version: map.version,
          file: map.file,
          names: map.names,
          sourceRoot: map.sourceRoot,
          sources: map.sources,
          sourcesContent: map.sourcesContent,
          mappings,
          ignoreList: map.ignoreList || map.x_google_ignoreList
        };
      }
      function OMapping(source, line, column, name) {
        return { source, line, column, name };
      }
      function GMapping(line, column) {
        return { line, column };
      }
      function traceSegmentInternal(segments, memo, line, column, bias) {
        let index = memoizedBinarySearch(segments, column, memo, line);
        if (found) {
          index = (bias === LEAST_UPPER_BOUND ? upperBound : lowerBound)(segments, column, index);
        } else if (bias === LEAST_UPPER_BOUND) index++;
        if (index === -1 || index === segments.length) return -1;
        return index;
      }
      function sliceGeneratedPositions(segments, memo, line, column, bias) {
        let min = traceSegmentInternal(segments, memo, line, column, GREATEST_LOWER_BOUND);
        if (!found && bias === LEAST_UPPER_BOUND) min++;
        if (min === -1 || min === segments.length) return [];
        const matchedColumn = found ? column : segments[min][COLUMN];
        if (!found) min = lowerBound(segments, matchedColumn, min);
        const max = upperBound(segments, matchedColumn, min);
        const result = [];
        for (; min <= max; min++) {
          const segment = segments[min];
          result.push(GMapping(segment[REV_GENERATED_LINE] + 1, segment[REV_GENERATED_COLUMN]));
        }
        return result;
      }
      function generatedPosition(map, source, line, column, bias, all) {
        var _a, _b;
        line--;
        if (line < 0) throw new Error(LINE_GTR_ZERO);
        if (column < 0) throw new Error(COL_GTR_EQ_ZERO);
        const { sources, resolvedSources } = map;
        let sourceIndex2 = sources.indexOf(source);
        if (sourceIndex2 === -1) sourceIndex2 = resolvedSources.indexOf(source);
        if (sourceIndex2 === -1) return all ? [] : GMapping(null, null);
        const bySourceMemos = (_a = cast(map))._bySourceMemos || (_a._bySourceMemos = sources.map(memoizedState));
        const generated = (_b = cast(map))._bySources || (_b._bySources = buildBySources(decodedMappings(map), bySourceMemos));
        const segments = generated[sourceIndex2][line];
        if (segments == null) return all ? [] : GMapping(null, null);
        const memo = bySourceMemos[sourceIndex2];
        if (all) return sliceGeneratedPositions(segments, memo, line, column, bias);
        const index = traceSegmentInternal(segments, memo, line, column, bias);
        if (index === -1) return GMapping(null, null);
        const segment = segments[index];
        return GMapping(segment[REV_GENERATED_LINE] + 1, segment[REV_GENERATED_COLUMN]);
      }
    });
  }
});

// node_modules/@jridgewell/gen-mapping/dist/gen-mapping.umd.js
var require_gen_mapping_umd = __commonJS({
  "node_modules/@jridgewell/gen-mapping/dist/gen-mapping.umd.js"(exports, module) {
    (function(global, factory) {
      if (typeof exports === "object" && typeof module !== "undefined") {
        factory(module, require_sourcemap_codec_umd(), require_trace_mapping_umd());
        module.exports = def(module);
      } else if (typeof define === "function" && define.amd) {
        define(["module", "@jridgewell/sourcemap-codec", "@jridgewell/trace-mapping"], function(mod) {
          factory.apply(this, arguments);
          mod.exports = def(mod);
        });
      } else {
        const mod = { exports: {} };
        factory(mod, global.sourcemapCodec, global.traceMapping);
        global = typeof globalThis !== "undefined" ? globalThis : global || self;
        global.genMapping = def(mod);
      }
      function def(m) {
        return "default" in m.exports ? m.exports.default : m.exports;
      }
    })(exports, function(module2, require_sourcemapCodec, require_traceMapping) {
      "use strict";
      var __create2 = Object.create;
      var __defProp2 = Object.defineProperty;
      var __getOwnPropDesc2 = Object.getOwnPropertyDescriptor;
      var __getOwnPropNames2 = Object.getOwnPropertyNames;
      var __getProtoOf2 = Object.getPrototypeOf;
      var __hasOwnProp2 = Object.prototype.hasOwnProperty;
      var __commonJS2 = (cb, mod) => function __require() {
        return mod || (0, cb[__getOwnPropNames2(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
      };
      var __export = (target, all) => {
        for (var name in all)
          __defProp2(target, name, { get: all[name], enumerable: true });
      };
      var __copyProps2 = (to, from, except, desc) => {
        if (from && typeof from === "object" || typeof from === "function") {
          for (let key of __getOwnPropNames2(from))
            if (!__hasOwnProp2.call(to, key) && key !== except)
              __defProp2(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc2(from, key)) || desc.enumerable });
        }
        return to;
      };
      var __toESM2 = (mod, isNodeMode, target) => (target = mod != null ? __create2(__getProtoOf2(mod)) : {}, __copyProps2(
        // If the importer is in node compatibility mode or this is not an ESM
        // file that has been converted to a CommonJS file using a Babel-
        // compatible transform (i.e. "__esModule" has not been set), then set
        // "default" to the CommonJS "module.exports" for node compatibility.
        isNodeMode || !mod || !mod.__esModule ? __defProp2(target, "default", { value: mod, enumerable: true }) : target,
        mod
      ));
      var __toCommonJS = (mod) => __copyProps2(__defProp2({}, "__esModule", { value: true }), mod);
      var require_sourcemap_codec = __commonJS2({
        "umd:@jridgewell/sourcemap-codec"(exports2, module22) {
          module22.exports = require_sourcemapCodec;
        }
      });
      var require_trace_mapping = __commonJS2({
        "umd:@jridgewell/trace-mapping"(exports2, module22) {
          module22.exports = require_traceMapping;
        }
      });
      var gen_mapping_exports = {};
      __export(gen_mapping_exports, {
        GenMapping: () => GenMapping,
        addMapping: () => addMapping,
        addSegment: () => addSegment,
        allMappings: () => allMappings,
        fromMap: () => fromMap,
        maybeAddMapping: () => maybeAddMapping,
        maybeAddSegment: () => maybeAddSegment,
        setIgnore: () => setIgnore,
        setSourceContent: () => setSourceContent,
        toDecodedMap: () => toDecodedMap,
        toEncodedMap: () => toEncodedMap
      });
      module2.exports = __toCommonJS(gen_mapping_exports);
      var SetArray = class {
        constructor() {
          this._indexes = { __proto__: null };
          this.array = [];
        }
      };
      function cast(set) {
        return set;
      }
      function get(setarr, key) {
        return cast(setarr)._indexes[key];
      }
      function put(setarr, key) {
        const index = get(setarr, key);
        if (index !== void 0) return index;
        const { array, _indexes: indexes } = cast(setarr);
        const length = array.push(key);
        return indexes[key] = length - 1;
      }
      function remove(setarr, key) {
        const index = get(setarr, key);
        if (index === void 0) return;
        const { array, _indexes: indexes } = cast(setarr);
        for (let i = index + 1; i < array.length; i++) {
          const k = array[i];
          array[i - 1] = k;
          indexes[k]--;
        }
        indexes[key] = void 0;
        array.pop();
      }
      var import_sourcemap_codec = __toESM2(require_sourcemap_codec());
      var import_trace_mapping = __toESM2(require_trace_mapping());
      var COLUMN = 0;
      var SOURCES_INDEX = 1;
      var SOURCE_LINE = 2;
      var SOURCE_COLUMN = 3;
      var NAMES_INDEX = 4;
      var NO_NAME = -1;
      var GenMapping = class {
        constructor({ file: file2, sourceRoot } = {}) {
          this._names = new SetArray();
          this._sources = new SetArray();
          this._sourcesContent = [];
          this._mappings = [];
          this.file = file2;
          this.sourceRoot = sourceRoot;
          this._ignoreList = new SetArray();
        }
      };
      function cast2(map) {
        return map;
      }
      function addSegment(map, genLine, genColumn, source, sourceLine, sourceColumn, name, content) {
        return addSegmentInternal(
          false,
          map,
          genLine,
          genColumn,
          source,
          sourceLine,
          sourceColumn,
          name,
          content
        );
      }
      function addMapping(map, mapping) {
        return addMappingInternal(false, map, mapping);
      }
      var maybeAddSegment = (map, genLine, genColumn, source, sourceLine, sourceColumn, name, content) => {
        return addSegmentInternal(
          true,
          map,
          genLine,
          genColumn,
          source,
          sourceLine,
          sourceColumn,
          name,
          content
        );
      };
      var maybeAddMapping = (map, mapping) => {
        return addMappingInternal(true, map, mapping);
      };
      function setSourceContent(map, source, content) {
        const {
          _sources: sources,
          _sourcesContent: sourcesContent
          // _originalScopes: originalScopes,
        } = cast2(map);
        const index = put(sources, source);
        sourcesContent[index] = content;
      }
      function setIgnore(map, source, ignore = true) {
        const {
          _sources: sources,
          _sourcesContent: sourcesContent,
          _ignoreList: ignoreList
          // _originalScopes: originalScopes,
        } = cast2(map);
        const index = put(sources, source);
        if (index === sourcesContent.length) sourcesContent[index] = null;
        if (ignore) put(ignoreList, index);
        else remove(ignoreList, index);
      }
      function toDecodedMap(map) {
        const {
          _mappings: mappings,
          _sources: sources,
          _sourcesContent: sourcesContent,
          _names: names,
          _ignoreList: ignoreList
          // _originalScopes: originalScopes,
          // _generatedRanges: generatedRanges,
        } = cast2(map);
        removeEmptyFinalLines(mappings);
        return {
          version: 3,
          file: map.file || void 0,
          names: names.array,
          sourceRoot: map.sourceRoot || void 0,
          sources: sources.array,
          sourcesContent,
          mappings,
          // originalScopes,
          // generatedRanges,
          ignoreList: ignoreList.array
        };
      }
      function toEncodedMap(map) {
        const decoded = toDecodedMap(map);
        return Object.assign({}, decoded, {
          // originalScopes: decoded.originalScopes.map((os) => encodeOriginalScopes(os)),
          // generatedRanges: encodeGeneratedRanges(decoded.generatedRanges as GeneratedRange[]),
          mappings: (0, import_sourcemap_codec.encode)(decoded.mappings)
        });
      }
      function fromMap(input) {
        const map = new import_trace_mapping.TraceMap(input);
        const gen = new GenMapping({ file: map.file, sourceRoot: map.sourceRoot });
        putAll(cast2(gen)._names, map.names);
        putAll(cast2(gen)._sources, map.sources);
        cast2(gen)._sourcesContent = map.sourcesContent || map.sources.map(() => null);
        cast2(gen)._mappings = (0, import_trace_mapping.decodedMappings)(map);
        if (map.ignoreList) putAll(cast2(gen)._ignoreList, map.ignoreList);
        return gen;
      }
      function allMappings(map) {
        const out2 = [];
        const { _mappings: mappings, _sources: sources, _names: names } = cast2(map);
        for (let i = 0; i < mappings.length; i++) {
          const line = mappings[i];
          for (let j = 0; j < line.length; j++) {
            const seg = line[j];
            const generated = { line: i + 1, column: seg[COLUMN] };
            let source = void 0;
            let original = void 0;
            let name = void 0;
            if (seg.length !== 1) {
              source = sources.array[seg[SOURCES_INDEX]];
              original = { line: seg[SOURCE_LINE] + 1, column: seg[SOURCE_COLUMN] };
              if (seg.length === 5) name = names.array[seg[NAMES_INDEX]];
            }
            out2.push({ generated, source, original, name });
          }
        }
        return out2;
      }
      function addSegmentInternal(skipable, map, genLine, genColumn, source, sourceLine, sourceColumn, name, content) {
        const {
          _mappings: mappings,
          _sources: sources,
          _sourcesContent: sourcesContent,
          _names: names
          // _originalScopes: originalScopes,
        } = cast2(map);
        const line = getIndex(mappings, genLine);
        const index = getColumnIndex(line, genColumn);
        if (!source) {
          if (skipable && skipSourceless(line, index)) return;
          return insert(line, index, [genColumn]);
        }
        assert(sourceLine);
        assert(sourceColumn);
        const sourcesIndex = put(sources, source);
        const namesIndex = name ? put(names, name) : NO_NAME;
        if (sourcesIndex === sourcesContent.length) sourcesContent[sourcesIndex] = content != null ? content : null;
        if (skipable && skipSource(line, index, sourcesIndex, sourceLine, sourceColumn, namesIndex)) {
          return;
        }
        return insert(
          line,
          index,
          name ? [genColumn, sourcesIndex, sourceLine, sourceColumn, namesIndex] : [genColumn, sourcesIndex, sourceLine, sourceColumn]
        );
      }
      function assert(_val) {
      }
      function getIndex(arr, index) {
        for (let i = arr.length; i <= index; i++) {
          arr[i] = [];
        }
        return arr[index];
      }
      function getColumnIndex(line, genColumn) {
        let index = line.length;
        for (let i = index - 1; i >= 0; index = i--) {
          const current2 = line[i];
          if (genColumn >= current2[COLUMN]) break;
        }
        return index;
      }
      function insert(array, index, value) {
        for (let i = array.length; i > index; i--) {
          array[i] = array[i - 1];
        }
        array[index] = value;
      }
      function removeEmptyFinalLines(mappings) {
        const { length } = mappings;
        let len = length;
        for (let i = len - 1; i >= 0; len = i, i--) {
          if (mappings[i].length > 0) break;
        }
        if (len < length) mappings.length = len;
      }
      function putAll(setarr, array) {
        for (let i = 0; i < array.length; i++) put(setarr, array[i]);
      }
      function skipSourceless(line, index) {
        if (index === 0) return true;
        const prev = line[index - 1];
        return prev.length === 1;
      }
      function skipSource(line, index, sourcesIndex, sourceLine, sourceColumn, namesIndex) {
        if (index === 0) return false;
        const prev = line[index - 1];
        if (prev.length === 1) return false;
        return sourcesIndex === prev[SOURCES_INDEX] && sourceLine === prev[SOURCE_LINE] && sourceColumn === prev[SOURCE_COLUMN] && namesIndex === (prev.length === 5 ? prev[NAMES_INDEX] : NO_NAME);
      }
      function addMappingInternal(skipable, map, mapping) {
        const { generated, source, original, name, content } = mapping;
        if (!source) {
          return addSegmentInternal(
            skipable,
            map,
            generated.line - 1,
            generated.column,
            null,
            null,
            null,
            null,
            null
          );
        }
        assert(original);
        return addSegmentInternal(
          skipable,
          map,
          generated.line - 1,
          generated.column,
          source,
          original.line - 1,
          original.column,
          name,
          content
        );
      }
    });
  }
});

// node_modules/sucrase/dist/computeSourceMap.js
var require_computeSourceMap = __commonJS({
  "node_modules/sucrase/dist/computeSourceMap.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _genmapping = require_gen_mapping_umd();
    var _charcodes = require_charcodes();
    function computeSourceMap({ code: generatedCode, mappings: rawMappings }, filePath, options, source, tokens) {
      const sourceColumns = computeSourceColumns(source, tokens);
      const map = new (0, _genmapping.GenMapping)({ file: options.compiledFilename });
      let tokenIndex = 0;
      let currentMapping = rawMappings[0];
      while (currentMapping === void 0 && tokenIndex < rawMappings.length - 1) {
        tokenIndex++;
        currentMapping = rawMappings[tokenIndex];
      }
      let line = 0;
      let lineStart = 0;
      if (currentMapping !== lineStart) {
        _genmapping.maybeAddSegment.call(void 0, map, line, 0, filePath, line, 0);
      }
      for (let i = 0; i < generatedCode.length; i++) {
        if (i === currentMapping) {
          const genColumn = currentMapping - lineStart;
          const sourceColumn = sourceColumns[tokenIndex];
          _genmapping.maybeAddSegment.call(void 0, map, line, genColumn, filePath, line, sourceColumn);
          while ((currentMapping === i || currentMapping === void 0) && tokenIndex < rawMappings.length - 1) {
            tokenIndex++;
            currentMapping = rawMappings[tokenIndex];
          }
        }
        if (generatedCode.charCodeAt(i) === _charcodes.charCodes.lineFeed) {
          line++;
          lineStart = i + 1;
          if (currentMapping !== lineStart) {
            _genmapping.maybeAddSegment.call(void 0, map, line, 0, filePath, line, 0);
          }
        }
      }
      const { sourceRoot, sourcesContent, ...sourceMap } = _genmapping.toEncodedMap.call(void 0, map);
      return sourceMap;
    }
    exports.default = computeSourceMap;
    function computeSourceColumns(code, tokens) {
      const sourceColumns = new Array(tokens.length);
      let tokenIndex = 0;
      let currentMapping = tokens[tokenIndex].start;
      let lineStart = 0;
      for (let i = 0; i < code.length; i++) {
        if (i === currentMapping) {
          sourceColumns[tokenIndex] = currentMapping - lineStart;
          tokenIndex++;
          currentMapping = tokens[tokenIndex].start;
        }
        if (code.charCodeAt(i) === _charcodes.charCodes.lineFeed) {
          lineStart = i + 1;
        }
      }
      return sourceColumns;
    }
  }
});

// node_modules/sucrase/dist/HelperManager.js
var require_HelperManager = __commonJS({
  "node_modules/sucrase/dist/HelperManager.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var HELPERS = {
      require: `
    import {createRequire as CREATE_REQUIRE_NAME} from "module";
    const require = CREATE_REQUIRE_NAME(import.meta.url);
  `,
      interopRequireWildcard: `
    function interopRequireWildcard(obj) {
      if (obj && obj.__esModule) {
        return obj;
      } else {
        var newObj = {};
        if (obj != null) {
          for (var key in obj) {
            if (Object.prototype.hasOwnProperty.call(obj, key)) {
              newObj[key] = obj[key];
            }
          }
        }
        newObj.default = obj;
        return newObj;
      }
    }
  `,
      interopRequireDefault: `
    function interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
  `,
      createNamedExportFrom: `
    function createNamedExportFrom(obj, localName, importedName) {
      Object.defineProperty(exports, localName, {enumerable: true, configurable: true, get: () => obj[importedName]});
    }
  `,
      // Note that TypeScript and Babel do this differently; TypeScript does a simple existence
      // check in the exports object and does a plain assignment, whereas Babel uses
      // defineProperty and builds an object of explicitly-exported names so that star exports can
      // always take lower precedence. For now, we do the easier TypeScript thing.
      createStarExport: `
    function createStarExport(obj) {
      Object.keys(obj)
        .filter((key) => key !== "default" && key !== "__esModule")
        .forEach((key) => {
          if (exports.hasOwnProperty(key)) {
            return;
          }
          Object.defineProperty(exports, key, {enumerable: true, configurable: true, get: () => obj[key]});
        });
    }
  `,
      nullishCoalesce: `
    function nullishCoalesce(lhs, rhsFn) {
      if (lhs != null) {
        return lhs;
      } else {
        return rhsFn();
      }
    }
  `,
      asyncNullishCoalesce: `
    async function asyncNullishCoalesce(lhs, rhsFn) {
      if (lhs != null) {
        return lhs;
      } else {
        return await rhsFn();
      }
    }
  `,
      optionalChain: `
    function optionalChain(ops) {
      let lastAccessLHS = undefined;
      let value = ops[0];
      let i = 1;
      while (i < ops.length) {
        const op = ops[i];
        const fn = ops[i + 1];
        i += 2;
        if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) {
          return undefined;
        }
        if (op === 'access' || op === 'optionalAccess') {
          lastAccessLHS = value;
          value = fn(value);
        } else if (op === 'call' || op === 'optionalCall') {
          value = fn((...args) => value.call(lastAccessLHS, ...args));
          lastAccessLHS = undefined;
        }
      }
      return value;
    }
  `,
      asyncOptionalChain: `
    async function asyncOptionalChain(ops) {
      let lastAccessLHS = undefined;
      let value = ops[0];
      let i = 1;
      while (i < ops.length) {
        const op = ops[i];
        const fn = ops[i + 1];
        i += 2;
        if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) {
          return undefined;
        }
        if (op === 'access' || op === 'optionalAccess') {
          lastAccessLHS = value;
          value = await fn(value);
        } else if (op === 'call' || op === 'optionalCall') {
          value = await fn((...args) => value.call(lastAccessLHS, ...args));
          lastAccessLHS = undefined;
        }
      }
      return value;
    }
  `,
      optionalChainDelete: `
    function optionalChainDelete(ops) {
      const result = OPTIONAL_CHAIN_NAME(ops);
      return result == null ? true : result;
    }
  `,
      asyncOptionalChainDelete: `
    async function asyncOptionalChainDelete(ops) {
      const result = await ASYNC_OPTIONAL_CHAIN_NAME(ops);
      return result == null ? true : result;
    }
  `
    };
    var HelperManager = class _HelperManager {
      __init() {
        this.helperNames = {};
      }
      __init2() {
        this.createRequireName = null;
      }
      constructor(nameManager) {
        ;
        this.nameManager = nameManager;
        _HelperManager.prototype.__init.call(this);
        _HelperManager.prototype.__init2.call(this);
      }
      getHelperName(baseName) {
        let helperName = this.helperNames[baseName];
        if (helperName) {
          return helperName;
        }
        helperName = this.nameManager.claimFreeName(`_${baseName}`);
        this.helperNames[baseName] = helperName;
        return helperName;
      }
      emitHelpers() {
        let resultCode = "";
        if (this.helperNames.optionalChainDelete) {
          this.getHelperName("optionalChain");
        }
        if (this.helperNames.asyncOptionalChainDelete) {
          this.getHelperName("asyncOptionalChain");
        }
        for (const [baseName, helperCodeTemplate] of Object.entries(HELPERS)) {
          const helperName = this.helperNames[baseName];
          let helperCode = helperCodeTemplate;
          if (baseName === "optionalChainDelete") {
            helperCode = helperCode.replace("OPTIONAL_CHAIN_NAME", this.helperNames.optionalChain);
          } else if (baseName === "asyncOptionalChainDelete") {
            helperCode = helperCode.replace(
              "ASYNC_OPTIONAL_CHAIN_NAME",
              this.helperNames.asyncOptionalChain
            );
          } else if (baseName === "require") {
            if (this.createRequireName === null) {
              this.createRequireName = this.nameManager.claimFreeName("_createRequire");
            }
            helperCode = helperCode.replace(/CREATE_REQUIRE_NAME/g, this.createRequireName);
          }
          if (helperName) {
            resultCode += " ";
            resultCode += helperCode.replace(baseName, helperName).replace(/\s+/g, " ").trim();
          }
        }
        return resultCode;
      }
    };
    exports.HelperManager = HelperManager;
  }
});

// node_modules/sucrase/dist/identifyShadowedGlobals.js
var require_identifyShadowedGlobals = __commonJS({
  "node_modules/sucrase/dist/identifyShadowedGlobals.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _tokenizer = require_tokenizer();
    var _types = require_types();
    function identifyShadowedGlobals(tokens, scopes, globalNames) {
      if (!hasShadowedGlobals(tokens, globalNames)) {
        return;
      }
      markShadowedGlobals(tokens, scopes, globalNames);
    }
    exports.default = identifyShadowedGlobals;
    function hasShadowedGlobals(tokens, globalNames) {
      for (const token of tokens.tokens) {
        if (token.type === _types.TokenType.name && !token.isType && _tokenizer.isNonTopLevelDeclaration.call(void 0, token) && globalNames.has(tokens.identifierNameForToken(token))) {
          return true;
        }
      }
      return false;
    }
    exports.hasShadowedGlobals = hasShadowedGlobals;
    function markShadowedGlobals(tokens, scopes, globalNames) {
      const scopeStack = [];
      let scopeIndex = scopes.length - 1;
      for (let i = tokens.tokens.length - 1; ; i--) {
        while (scopeStack.length > 0 && scopeStack[scopeStack.length - 1].startTokenIndex === i + 1) {
          scopeStack.pop();
        }
        while (scopeIndex >= 0 && scopes[scopeIndex].endTokenIndex === i + 1) {
          scopeStack.push(scopes[scopeIndex]);
          scopeIndex--;
        }
        if (i < 0) {
          break;
        }
        const token = tokens.tokens[i];
        const name = tokens.identifierNameForToken(token);
        if (scopeStack.length > 1 && !token.isType && token.type === _types.TokenType.name && globalNames.has(name)) {
          if (_tokenizer.isBlockScopedDeclaration.call(void 0, token)) {
            markShadowedForScope(scopeStack[scopeStack.length - 1], tokens, name);
          } else if (_tokenizer.isFunctionScopedDeclaration.call(void 0, token)) {
            let stackIndex = scopeStack.length - 1;
            while (stackIndex > 0 && !scopeStack[stackIndex].isFunctionScope) {
              stackIndex--;
            }
            if (stackIndex < 0) {
              throw new Error("Did not find parent function scope.");
            }
            markShadowedForScope(scopeStack[stackIndex], tokens, name);
          }
        }
      }
      if (scopeStack.length > 0) {
        throw new Error("Expected empty scope stack after processing file.");
      }
    }
    function markShadowedForScope(scope, tokens, name) {
      for (let i = scope.startTokenIndex; i < scope.endTokenIndex; i++) {
        const token = tokens.tokens[i];
        if ((token.type === _types.TokenType.name || token.type === _types.TokenType.jsxName) && tokens.identifierNameForToken(token) === name) {
          token.shadowsGlobal = true;
        }
      }
    }
  }
});

// node_modules/sucrase/dist/util/getIdentifierNames.js
var require_getIdentifierNames = __commonJS({
  "node_modules/sucrase/dist/util/getIdentifierNames.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _types = require_types();
    function getIdentifierNames(code, tokens) {
      const names = [];
      for (const token of tokens) {
        if (token.type === _types.TokenType.name) {
          names.push(code.slice(token.start, token.end));
        }
      }
      return names;
    }
    exports.default = getIdentifierNames;
  }
});

// node_modules/sucrase/dist/NameManager.js
var require_NameManager = __commonJS({
  "node_modules/sucrase/dist/NameManager.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _getIdentifierNames = require_getIdentifierNames();
    var _getIdentifierNames2 = _interopRequireDefault(_getIdentifierNames);
    var NameManager = class _NameManager {
      __init() {
        this.usedNames = /* @__PURE__ */ new Set();
      }
      constructor(code, tokens) {
        ;
        _NameManager.prototype.__init.call(this);
        this.usedNames = new Set(_getIdentifierNames2.default.call(void 0, code, tokens));
      }
      claimFreeName(name) {
        const newName = this.findFreeName(name);
        this.usedNames.add(newName);
        return newName;
      }
      findFreeName(name) {
        if (!this.usedNames.has(name)) {
          return name;
        }
        let suffixNum = 2;
        while (this.usedNames.has(name + String(suffixNum))) {
          suffixNum++;
        }
        return name + String(suffixNum);
      }
    };
    exports.default = NameManager;
  }
});

// node_modules/ts-interface-checker/dist/util.js
var require_util2 = __commonJS({
  "node_modules/ts-interface-checker/dist/util.js"(exports) {
    "use strict";
    var __extends = exports && exports.__extends || /* @__PURE__ */ function() {
      var extendStatics = function(d, b) {
        extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d2, b2) {
          d2.__proto__ = b2;
        } || function(d2, b2) {
          for (var p in b2) if (b2.hasOwnProperty(p)) d2[p] = b2[p];
        };
        return extendStatics(d, b);
      };
      return function(d, b) {
        extendStatics(d, b);
        function __() {
          this.constructor = d;
        }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
      };
    }();
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.DetailContext = exports.NoopContext = exports.VError = void 0;
    var VError = (
      /** @class */
      function(_super) {
        __extends(VError2, _super);
        function VError2(path, message) {
          var _this = _super.call(this, message) || this;
          _this.path = path;
          Object.setPrototypeOf(_this, VError2.prototype);
          return _this;
        }
        return VError2;
      }(Error)
    );
    exports.VError = VError;
    var NoopContext = (
      /** @class */
      function() {
        function NoopContext2() {
        }
        NoopContext2.prototype.fail = function(relPath, message, score) {
          return false;
        };
        NoopContext2.prototype.unionResolver = function() {
          return this;
        };
        NoopContext2.prototype.createContext = function() {
          return this;
        };
        NoopContext2.prototype.resolveUnion = function(ur) {
        };
        return NoopContext2;
      }()
    );
    exports.NoopContext = NoopContext;
    var DetailContext = (
      /** @class */
      function() {
        function DetailContext2() {
          this._propNames = [""];
          this._messages = [null];
          this._score = 0;
        }
        DetailContext2.prototype.fail = function(relPath, message, score) {
          this._propNames.push(relPath);
          this._messages.push(message);
          this._score += score;
          return false;
        };
        DetailContext2.prototype.unionResolver = function() {
          return new DetailUnionResolver();
        };
        DetailContext2.prototype.resolveUnion = function(unionResolver) {
          var _a, _b;
          var u = unionResolver;
          var best = null;
          for (var _i = 0, _c = u.contexts; _i < _c.length; _i++) {
            var ctx = _c[_i];
            if (!best || ctx._score >= best._score) {
              best = ctx;
            }
          }
          if (best && best._score > 0) {
            (_a = this._propNames).push.apply(_a, best._propNames);
            (_b = this._messages).push.apply(_b, best._messages);
          }
        };
        DetailContext2.prototype.getError = function(path) {
          var msgParts = [];
          for (var i = this._propNames.length - 1; i >= 0; i--) {
            var p = this._propNames[i];
            path += typeof p === "number" ? "[" + p + "]" : p ? "." + p : "";
            var m = this._messages[i];
            if (m) {
              msgParts.push(path + " " + m);
            }
          }
          return new VError(path, msgParts.join("; "));
        };
        DetailContext2.prototype.getErrorDetail = function(path) {
          var details = [];
          for (var i = this._propNames.length - 1; i >= 0; i--) {
            var p = this._propNames[i];
            path += typeof p === "number" ? "[" + p + "]" : p ? "." + p : "";
            var message = this._messages[i];
            if (message) {
              details.push({ path, message });
            }
          }
          var detail = null;
          for (var i = details.length - 1; i >= 0; i--) {
            if (detail) {
              details[i].nested = [detail];
            }
            detail = details[i];
          }
          return detail;
        };
        return DetailContext2;
      }()
    );
    exports.DetailContext = DetailContext;
    var DetailUnionResolver = (
      /** @class */
      function() {
        function DetailUnionResolver2() {
          this.contexts = [];
        }
        DetailUnionResolver2.prototype.createContext = function() {
          var ctx = new DetailContext();
          this.contexts.push(ctx);
          return ctx;
        };
        return DetailUnionResolver2;
      }()
    );
  }
});

// node_modules/ts-interface-checker/dist/types.js
var require_types2 = __commonJS({
  "node_modules/ts-interface-checker/dist/types.js"(exports) {
    "use strict";
    var __extends = exports && exports.__extends || /* @__PURE__ */ function() {
      var extendStatics = function(d, b) {
        extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d2, b2) {
          d2.__proto__ = b2;
        } || function(d2, b2) {
          for (var p in b2) if (b2.hasOwnProperty(p)) d2[p] = b2[p];
        };
        return extendStatics(d, b);
      };
      return function(d, b) {
        extendStatics(d, b);
        function __() {
          this.constructor = d;
        }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
      };
    }();
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.basicTypes = exports.BasicType = exports.TParamList = exports.TParam = exports.param = exports.TFunc = exports.func = exports.TProp = exports.TOptional = exports.opt = exports.TIface = exports.iface = exports.TEnumLiteral = exports.enumlit = exports.TEnumType = exports.enumtype = exports.TIntersection = exports.intersection = exports.TUnion = exports.union = exports.TTuple = exports.tuple = exports.TArray = exports.array = exports.TLiteral = exports.lit = exports.TName = exports.name = exports.TType = void 0;
    var util_1 = require_util2();
    var TType = (
      /** @class */
      /* @__PURE__ */ function() {
        function TType2() {
        }
        return TType2;
      }()
    );
    exports.TType = TType;
    function parseSpec(typeSpec) {
      return typeof typeSpec === "string" ? name(typeSpec) : typeSpec;
    }
    function getNamedType(suite, name2) {
      var ttype = suite[name2];
      if (!ttype) {
        throw new Error("Unknown type " + name2);
      }
      return ttype;
    }
    function name(value) {
      return new TName(value);
    }
    exports.name = name;
    var TName = (
      /** @class */
      function(_super) {
        __extends(TName2, _super);
        function TName2(name2) {
          var _this = _super.call(this) || this;
          _this.name = name2;
          _this._failMsg = "is not a " + name2;
          return _this;
        }
        TName2.prototype.getChecker = function(suite, strict, allowedProps) {
          var _this = this;
          var ttype = getNamedType(suite, this.name);
          var checker = ttype.getChecker(suite, strict, allowedProps);
          if (ttype instanceof BasicType || ttype instanceof TName2) {
            return checker;
          }
          return function(value, ctx) {
            return checker(value, ctx) ? true : ctx.fail(null, _this._failMsg, 0);
          };
        };
        return TName2;
      }(TType)
    );
    exports.TName = TName;
    function lit(value) {
      return new TLiteral(value);
    }
    exports.lit = lit;
    var TLiteral = (
      /** @class */
      function(_super) {
        __extends(TLiteral2, _super);
        function TLiteral2(value) {
          var _this = _super.call(this) || this;
          _this.value = value;
          _this.name = JSON.stringify(value);
          _this._failMsg = "is not " + _this.name;
          return _this;
        }
        TLiteral2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          return function(value, ctx) {
            return value === _this.value ? true : ctx.fail(null, _this._failMsg, -1);
          };
        };
        return TLiteral2;
      }(TType)
    );
    exports.TLiteral = TLiteral;
    function array(typeSpec) {
      return new TArray(parseSpec(typeSpec));
    }
    exports.array = array;
    var TArray = (
      /** @class */
      function(_super) {
        __extends(TArray2, _super);
        function TArray2(ttype) {
          var _this = _super.call(this) || this;
          _this.ttype = ttype;
          return _this;
        }
        TArray2.prototype.getChecker = function(suite, strict) {
          var itemChecker = this.ttype.getChecker(suite, strict);
          return function(value, ctx) {
            if (!Array.isArray(value)) {
              return ctx.fail(null, "is not an array", 0);
            }
            for (var i = 0; i < value.length; i++) {
              var ok = itemChecker(value[i], ctx);
              if (!ok) {
                return ctx.fail(i, null, 1);
              }
            }
            return true;
          };
        };
        return TArray2;
      }(TType)
    );
    exports.TArray = TArray;
    function tuple() {
      var typeSpec = [];
      for (var _i2 = 0; _i2 < arguments.length; _i2++) {
        typeSpec[_i2] = arguments[_i2];
      }
      return new TTuple(typeSpec.map(function(t) {
        return parseSpec(t);
      }));
    }
    exports.tuple = tuple;
    var TTuple = (
      /** @class */
      function(_super) {
        __extends(TTuple2, _super);
        function TTuple2(ttypes) {
          var _this = _super.call(this) || this;
          _this.ttypes = ttypes;
          return _this;
        }
        TTuple2.prototype.getChecker = function(suite, strict) {
          var itemCheckers = this.ttypes.map(function(t) {
            return t.getChecker(suite, strict);
          });
          var checker = function(value, ctx) {
            if (!Array.isArray(value)) {
              return ctx.fail(null, "is not an array", 0);
            }
            for (var i = 0; i < itemCheckers.length; i++) {
              var ok = itemCheckers[i](value[i], ctx);
              if (!ok) {
                return ctx.fail(i, null, 1);
              }
            }
            return true;
          };
          if (!strict) {
            return checker;
          }
          return function(value, ctx) {
            if (!checker(value, ctx)) {
              return false;
            }
            return value.length <= itemCheckers.length ? true : ctx.fail(itemCheckers.length, "is extraneous", 2);
          };
        };
        return TTuple2;
      }(TType)
    );
    exports.TTuple = TTuple;
    function union() {
      var typeSpec = [];
      for (var _i2 = 0; _i2 < arguments.length; _i2++) {
        typeSpec[_i2] = arguments[_i2];
      }
      return new TUnion(typeSpec.map(function(t) {
        return parseSpec(t);
      }));
    }
    exports.union = union;
    var TUnion = (
      /** @class */
      function(_super) {
        __extends(TUnion2, _super);
        function TUnion2(ttypes) {
          var _this = _super.call(this) || this;
          _this.ttypes = ttypes;
          var names = ttypes.map(function(t) {
            return t instanceof TName || t instanceof TLiteral ? t.name : null;
          }).filter(function(n) {
            return n;
          });
          var otherTypes = ttypes.length - names.length;
          if (names.length) {
            if (otherTypes > 0) {
              names.push(otherTypes + " more");
            }
            _this._failMsg = "is none of " + names.join(", ");
          } else {
            _this._failMsg = "is none of " + otherTypes + " types";
          }
          return _this;
        }
        TUnion2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          var itemCheckers = this.ttypes.map(function(t) {
            return t.getChecker(suite, strict);
          });
          return function(value, ctx) {
            var ur = ctx.unionResolver();
            for (var i = 0; i < itemCheckers.length; i++) {
              var ok = itemCheckers[i](value, ur.createContext());
              if (ok) {
                return true;
              }
            }
            ctx.resolveUnion(ur);
            return ctx.fail(null, _this._failMsg, 0);
          };
        };
        return TUnion2;
      }(TType)
    );
    exports.TUnion = TUnion;
    function intersection() {
      var typeSpec = [];
      for (var _i2 = 0; _i2 < arguments.length; _i2++) {
        typeSpec[_i2] = arguments[_i2];
      }
      return new TIntersection(typeSpec.map(function(t) {
        return parseSpec(t);
      }));
    }
    exports.intersection = intersection;
    var TIntersection = (
      /** @class */
      function(_super) {
        __extends(TIntersection2, _super);
        function TIntersection2(ttypes) {
          var _this = _super.call(this) || this;
          _this.ttypes = ttypes;
          return _this;
        }
        TIntersection2.prototype.getChecker = function(suite, strict) {
          var allowedProps = /* @__PURE__ */ new Set();
          var itemCheckers = this.ttypes.map(function(t) {
            return t.getChecker(suite, strict, allowedProps);
          });
          return function(value, ctx) {
            var ok = itemCheckers.every(function(checker) {
              return checker(value, ctx);
            });
            if (ok) {
              return true;
            }
            return ctx.fail(null, null, 0);
          };
        };
        return TIntersection2;
      }(TType)
    );
    exports.TIntersection = TIntersection;
    function enumtype(values) {
      return new TEnumType(values);
    }
    exports.enumtype = enumtype;
    var TEnumType = (
      /** @class */
      function(_super) {
        __extends(TEnumType2, _super);
        function TEnumType2(members) {
          var _this = _super.call(this) || this;
          _this.members = members;
          _this.validValues = /* @__PURE__ */ new Set();
          _this._failMsg = "is not a valid enum value";
          _this.validValues = new Set(Object.keys(members).map(function(name2) {
            return members[name2];
          }));
          return _this;
        }
        TEnumType2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          return function(value, ctx) {
            return _this.validValues.has(value) ? true : ctx.fail(null, _this._failMsg, 0);
          };
        };
        return TEnumType2;
      }(TType)
    );
    exports.TEnumType = TEnumType;
    function enumlit(name2, prop) {
      return new TEnumLiteral(name2, prop);
    }
    exports.enumlit = enumlit;
    var TEnumLiteral = (
      /** @class */
      function(_super) {
        __extends(TEnumLiteral2, _super);
        function TEnumLiteral2(enumName, prop) {
          var _this = _super.call(this) || this;
          _this.enumName = enumName;
          _this.prop = prop;
          _this._failMsg = "is not " + enumName + "." + prop;
          return _this;
        }
        TEnumLiteral2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          var ttype = getNamedType(suite, this.enumName);
          if (!(ttype instanceof TEnumType)) {
            throw new Error("Type " + this.enumName + " used in enumlit is not an enum type");
          }
          var val = ttype.members[this.prop];
          if (!ttype.members.hasOwnProperty(this.prop)) {
            throw new Error("Unknown value " + this.enumName + "." + this.prop + " used in enumlit");
          }
          return function(value, ctx) {
            return value === val ? true : ctx.fail(null, _this._failMsg, -1);
          };
        };
        return TEnumLiteral2;
      }(TType)
    );
    exports.TEnumLiteral = TEnumLiteral;
    function makeIfaceProps(props) {
      return Object.keys(props).map(function(name2) {
        return makeIfaceProp(name2, props[name2]);
      });
    }
    function makeIfaceProp(name2, prop) {
      return prop instanceof TOptional ? new TProp(name2, prop.ttype, true) : new TProp(name2, parseSpec(prop), false);
    }
    function iface(bases, props) {
      return new TIface(bases, makeIfaceProps(props));
    }
    exports.iface = iface;
    var TIface = (
      /** @class */
      function(_super) {
        __extends(TIface2, _super);
        function TIface2(bases, props) {
          var _this = _super.call(this) || this;
          _this.bases = bases;
          _this.props = props;
          _this.propSet = new Set(props.map(function(p) {
            return p.name;
          }));
          return _this;
        }
        TIface2.prototype.getChecker = function(suite, strict, allowedProps) {
          var _this = this;
          var baseCheckers = this.bases.map(function(b) {
            return getNamedType(suite, b).getChecker(suite, strict);
          });
          var propCheckers = this.props.map(function(prop) {
            return prop.ttype.getChecker(suite, strict);
          });
          var testCtx = new util_1.NoopContext();
          var isPropRequired = this.props.map(function(prop, i) {
            return !prop.isOpt && !propCheckers[i](void 0, testCtx);
          });
          var checker = function(value, ctx) {
            if (typeof value !== "object" || value === null) {
              return ctx.fail(null, "is not an object", 0);
            }
            for (var i = 0; i < baseCheckers.length; i++) {
              if (!baseCheckers[i](value, ctx)) {
                return false;
              }
            }
            for (var i = 0; i < propCheckers.length; i++) {
              var name_1 = _this.props[i].name;
              var v = value[name_1];
              if (v === void 0) {
                if (isPropRequired[i]) {
                  return ctx.fail(name_1, "is missing", 1);
                }
              } else {
                var ok = propCheckers[i](v, ctx);
                if (!ok) {
                  return ctx.fail(name_1, null, 1);
                }
              }
            }
            return true;
          };
          if (!strict) {
            return checker;
          }
          var propSet = this.propSet;
          if (allowedProps) {
            this.propSet.forEach(function(prop) {
              return allowedProps.add(prop);
            });
            propSet = allowedProps;
          }
          return function(value, ctx) {
            if (!checker(value, ctx)) {
              return false;
            }
            for (var prop in value) {
              if (!propSet.has(prop)) {
                return ctx.fail(prop, "is extraneous", 2);
              }
            }
            return true;
          };
        };
        return TIface2;
      }(TType)
    );
    exports.TIface = TIface;
    function opt(typeSpec) {
      return new TOptional(parseSpec(typeSpec));
    }
    exports.opt = opt;
    var TOptional = (
      /** @class */
      function(_super) {
        __extends(TOptional2, _super);
        function TOptional2(ttype) {
          var _this = _super.call(this) || this;
          _this.ttype = ttype;
          return _this;
        }
        TOptional2.prototype.getChecker = function(suite, strict) {
          var itemChecker = this.ttype.getChecker(suite, strict);
          return function(value, ctx) {
            return value === void 0 || itemChecker(value, ctx);
          };
        };
        return TOptional2;
      }(TType)
    );
    exports.TOptional = TOptional;
    var TProp = (
      /** @class */
      /* @__PURE__ */ function() {
        function TProp2(name2, ttype, isOpt) {
          this.name = name2;
          this.ttype = ttype;
          this.isOpt = isOpt;
        }
        return TProp2;
      }()
    );
    exports.TProp = TProp;
    function func(resultSpec) {
      var params = [];
      for (var _i2 = 1; _i2 < arguments.length; _i2++) {
        params[_i2 - 1] = arguments[_i2];
      }
      return new TFunc(new TParamList(params), parseSpec(resultSpec));
    }
    exports.func = func;
    var TFunc = (
      /** @class */
      function(_super) {
        __extends(TFunc2, _super);
        function TFunc2(paramList, result) {
          var _this = _super.call(this) || this;
          _this.paramList = paramList;
          _this.result = result;
          return _this;
        }
        TFunc2.prototype.getChecker = function(suite, strict) {
          return function(value, ctx) {
            return typeof value === "function" ? true : ctx.fail(null, "is not a function", 0);
          };
        };
        return TFunc2;
      }(TType)
    );
    exports.TFunc = TFunc;
    function param(name2, typeSpec, isOpt) {
      return new TParam(name2, parseSpec(typeSpec), Boolean(isOpt));
    }
    exports.param = param;
    var TParam = (
      /** @class */
      /* @__PURE__ */ function() {
        function TParam2(name2, ttype, isOpt) {
          this.name = name2;
          this.ttype = ttype;
          this.isOpt = isOpt;
        }
        return TParam2;
      }()
    );
    exports.TParam = TParam;
    var TParamList = (
      /** @class */
      function(_super) {
        __extends(TParamList2, _super);
        function TParamList2(params) {
          var _this = _super.call(this) || this;
          _this.params = params;
          return _this;
        }
        TParamList2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          var itemCheckers = this.params.map(function(t) {
            return t.ttype.getChecker(suite, strict);
          });
          var testCtx = new util_1.NoopContext();
          var isParamRequired = this.params.map(function(param2, i) {
            return !param2.isOpt && !itemCheckers[i](void 0, testCtx);
          });
          var checker = function(value, ctx) {
            if (!Array.isArray(value)) {
              return ctx.fail(null, "is not an array", 0);
            }
            for (var i = 0; i < itemCheckers.length; i++) {
              var p = _this.params[i];
              if (value[i] === void 0) {
                if (isParamRequired[i]) {
                  return ctx.fail(p.name, "is missing", 1);
                }
              } else {
                var ok = itemCheckers[i](value[i], ctx);
                if (!ok) {
                  return ctx.fail(p.name, null, 1);
                }
              }
            }
            return true;
          };
          if (!strict) {
            return checker;
          }
          return function(value, ctx) {
            if (!checker(value, ctx)) {
              return false;
            }
            return value.length <= itemCheckers.length ? true : ctx.fail(itemCheckers.length, "is extraneous", 2);
          };
        };
        return TParamList2;
      }(TType)
    );
    exports.TParamList = TParamList;
    var BasicType = (
      /** @class */
      function(_super) {
        __extends(BasicType2, _super);
        function BasicType2(validator, message) {
          var _this = _super.call(this) || this;
          _this.validator = validator;
          _this.message = message;
          return _this;
        }
        BasicType2.prototype.getChecker = function(suite, strict) {
          var _this = this;
          return function(value, ctx) {
            return _this.validator(value) ? true : ctx.fail(null, _this.message, 0);
          };
        };
        return BasicType2;
      }(TType)
    );
    exports.BasicType = BasicType;
    exports.basicTypes = {
      any: new BasicType(function(v) {
        return true;
      }, "is invalid"),
      number: new BasicType(function(v) {
        return typeof v === "number";
      }, "is not a number"),
      object: new BasicType(function(v) {
        return typeof v === "object" && v;
      }, "is not an object"),
      boolean: new BasicType(function(v) {
        return typeof v === "boolean";
      }, "is not a boolean"),
      string: new BasicType(function(v) {
        return typeof v === "string";
      }, "is not a string"),
      symbol: new BasicType(function(v) {
        return typeof v === "symbol";
      }, "is not a symbol"),
      void: new BasicType(function(v) {
        return v == null;
      }, "is not void"),
      undefined: new BasicType(function(v) {
        return v === void 0;
      }, "is not undefined"),
      null: new BasicType(function(v) {
        return v === null;
      }, "is not null"),
      never: new BasicType(function(v) {
        return false;
      }, "is unexpected"),
      Date: new BasicType(getIsNativeChecker("[object Date]"), "is not a Date"),
      RegExp: new BasicType(getIsNativeChecker("[object RegExp]"), "is not a RegExp")
    };
    var nativeToString = Object.prototype.toString;
    function getIsNativeChecker(tag) {
      return function(v) {
        return typeof v === "object" && v && nativeToString.call(v) === tag;
      };
    }
    if (typeof Buffer !== "undefined") {
      exports.basicTypes.Buffer = new BasicType(function(v) {
        return Buffer.isBuffer(v);
      }, "is not a Buffer");
    }
    var _loop_1 = function(array_12) {
      exports.basicTypes[array_12.name] = new BasicType(function(v) {
        return v instanceof array_12;
      }, "is not a " + array_12.name);
    };
    for (_i = 0, _a = [
      Int8Array,
      Uint8Array,
      Uint8ClampedArray,
      Int16Array,
      Uint16Array,
      Int32Array,
      Uint32Array,
      Float32Array,
      Float64Array,
      ArrayBuffer
    ]; _i < _a.length; _i++) {
      array_1 = _a[_i];
      _loop_1(array_1);
    }
    var array_1;
    var _i;
    var _a;
  }
});

// node_modules/ts-interface-checker/dist/index.js
var require_dist = __commonJS({
  "node_modules/ts-interface-checker/dist/index.js"(exports) {
    "use strict";
    var __spreadArrays = exports && exports.__spreadArrays || function() {
      for (var s = 0, i = 0, il = arguments.length; i < il; i++) s += arguments[i].length;
      for (var r = Array(s), k = 0, i = 0; i < il; i++)
        for (var a = arguments[i], j = 0, jl = a.length; j < jl; j++, k++)
          r[k] = a[j];
      return r;
    };
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.Checker = exports.createCheckers = void 0;
    var types_1 = require_types2();
    var util_1 = require_util2();
    var types_2 = require_types2();
    Object.defineProperty(exports, "TArray", { enumerable: true, get: function() {
      return types_2.TArray;
    } });
    Object.defineProperty(exports, "TEnumType", { enumerable: true, get: function() {
      return types_2.TEnumType;
    } });
    Object.defineProperty(exports, "TEnumLiteral", { enumerable: true, get: function() {
      return types_2.TEnumLiteral;
    } });
    Object.defineProperty(exports, "TFunc", { enumerable: true, get: function() {
      return types_2.TFunc;
    } });
    Object.defineProperty(exports, "TIface", { enumerable: true, get: function() {
      return types_2.TIface;
    } });
    Object.defineProperty(exports, "TLiteral", { enumerable: true, get: function() {
      return types_2.TLiteral;
    } });
    Object.defineProperty(exports, "TName", { enumerable: true, get: function() {
      return types_2.TName;
    } });
    Object.defineProperty(exports, "TOptional", { enumerable: true, get: function() {
      return types_2.TOptional;
    } });
    Object.defineProperty(exports, "TParam", { enumerable: true, get: function() {
      return types_2.TParam;
    } });
    Object.defineProperty(exports, "TParamList", { enumerable: true, get: function() {
      return types_2.TParamList;
    } });
    Object.defineProperty(exports, "TProp", { enumerable: true, get: function() {
      return types_2.TProp;
    } });
    Object.defineProperty(exports, "TTuple", { enumerable: true, get: function() {
      return types_2.TTuple;
    } });
    Object.defineProperty(exports, "TType", { enumerable: true, get: function() {
      return types_2.TType;
    } });
    Object.defineProperty(exports, "TUnion", { enumerable: true, get: function() {
      return types_2.TUnion;
    } });
    Object.defineProperty(exports, "TIntersection", { enumerable: true, get: function() {
      return types_2.TIntersection;
    } });
    Object.defineProperty(exports, "array", { enumerable: true, get: function() {
      return types_2.array;
    } });
    Object.defineProperty(exports, "enumlit", { enumerable: true, get: function() {
      return types_2.enumlit;
    } });
    Object.defineProperty(exports, "enumtype", { enumerable: true, get: function() {
      return types_2.enumtype;
    } });
    Object.defineProperty(exports, "func", { enumerable: true, get: function() {
      return types_2.func;
    } });
    Object.defineProperty(exports, "iface", { enumerable: true, get: function() {
      return types_2.iface;
    } });
    Object.defineProperty(exports, "lit", { enumerable: true, get: function() {
      return types_2.lit;
    } });
    Object.defineProperty(exports, "name", { enumerable: true, get: function() {
      return types_2.name;
    } });
    Object.defineProperty(exports, "opt", { enumerable: true, get: function() {
      return types_2.opt;
    } });
    Object.defineProperty(exports, "param", { enumerable: true, get: function() {
      return types_2.param;
    } });
    Object.defineProperty(exports, "tuple", { enumerable: true, get: function() {
      return types_2.tuple;
    } });
    Object.defineProperty(exports, "union", { enumerable: true, get: function() {
      return types_2.union;
    } });
    Object.defineProperty(exports, "intersection", { enumerable: true, get: function() {
      return types_2.intersection;
    } });
    Object.defineProperty(exports, "BasicType", { enumerable: true, get: function() {
      return types_2.BasicType;
    } });
    var util_2 = require_util2();
    Object.defineProperty(exports, "VError", { enumerable: true, get: function() {
      return util_2.VError;
    } });
    function createCheckers() {
      var typeSuite = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        typeSuite[_i] = arguments[_i];
      }
      var fullSuite = Object.assign.apply(Object, __spreadArrays([{}, types_1.basicTypes], typeSuite));
      var checkers = {};
      for (var _a = 0, typeSuite_1 = typeSuite; _a < typeSuite_1.length; _a++) {
        var suite_1 = typeSuite_1[_a];
        for (var _b = 0, _c = Object.keys(suite_1); _b < _c.length; _b++) {
          var name = _c[_b];
          checkers[name] = new Checker(fullSuite, suite_1[name]);
        }
      }
      return checkers;
    }
    exports.createCheckers = createCheckers;
    var Checker = (
      /** @class */
      function() {
        function Checker2(suite, ttype, _path) {
          if (_path === void 0) {
            _path = "value";
          }
          this.suite = suite;
          this.ttype = ttype;
          this._path = _path;
          this.props = /* @__PURE__ */ new Map();
          if (ttype instanceof types_1.TIface) {
            for (var _i = 0, _a = ttype.props; _i < _a.length; _i++) {
              var p = _a[_i];
              this.props.set(p.name, p.ttype);
            }
          }
          this.checkerPlain = this.ttype.getChecker(suite, false);
          this.checkerStrict = this.ttype.getChecker(suite, true);
        }
        Checker2.prototype.setReportedPath = function(path) {
          this._path = path;
        };
        Checker2.prototype.check = function(value) {
          return this._doCheck(this.checkerPlain, value);
        };
        Checker2.prototype.test = function(value) {
          return this.checkerPlain(value, new util_1.NoopContext());
        };
        Checker2.prototype.validate = function(value) {
          return this._doValidate(this.checkerPlain, value);
        };
        Checker2.prototype.strictCheck = function(value) {
          return this._doCheck(this.checkerStrict, value);
        };
        Checker2.prototype.strictTest = function(value) {
          return this.checkerStrict(value, new util_1.NoopContext());
        };
        Checker2.prototype.strictValidate = function(value) {
          return this._doValidate(this.checkerStrict, value);
        };
        Checker2.prototype.getProp = function(prop) {
          var ttype = this.props.get(prop);
          if (!ttype) {
            throw new Error("Type has no property " + prop);
          }
          return new Checker2(this.suite, ttype, this._path + "." + prop);
        };
        Checker2.prototype.methodArgs = function(methodName) {
          var tfunc = this._getMethod(methodName);
          return new Checker2(this.suite, tfunc.paramList);
        };
        Checker2.prototype.methodResult = function(methodName) {
          var tfunc = this._getMethod(methodName);
          return new Checker2(this.suite, tfunc.result);
        };
        Checker2.prototype.getArgs = function() {
          if (!(this.ttype instanceof types_1.TFunc)) {
            throw new Error("getArgs() applied to non-function");
          }
          return new Checker2(this.suite, this.ttype.paramList);
        };
        Checker2.prototype.getResult = function() {
          if (!(this.ttype instanceof types_1.TFunc)) {
            throw new Error("getResult() applied to non-function");
          }
          return new Checker2(this.suite, this.ttype.result);
        };
        Checker2.prototype.getType = function() {
          return this.ttype;
        };
        Checker2.prototype._doCheck = function(checkerFunc, value) {
          var noopCtx = new util_1.NoopContext();
          if (!checkerFunc(value, noopCtx)) {
            var detailCtx = new util_1.DetailContext();
            checkerFunc(value, detailCtx);
            throw detailCtx.getError(this._path);
          }
        };
        Checker2.prototype._doValidate = function(checkerFunc, value) {
          var noopCtx = new util_1.NoopContext();
          if (checkerFunc(value, noopCtx)) {
            return null;
          }
          var detailCtx = new util_1.DetailContext();
          checkerFunc(value, detailCtx);
          return detailCtx.getErrorDetail(this._path);
        };
        Checker2.prototype._getMethod = function(methodName) {
          var ttype = this.props.get(methodName);
          if (!ttype) {
            throw new Error("Type has no property " + methodName);
          }
          if (!(ttype instanceof types_1.TFunc)) {
            throw new Error("Property " + methodName + " is not a method");
          }
          return ttype;
        };
        return Checker2;
      }()
    );
    exports.Checker = Checker;
  }
});

// node_modules/sucrase/dist/Options-gen-types.js
var require_Options_gen_types = __commonJS({
  "node_modules/sucrase/dist/Options-gen-types.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireWildcard(obj) {
      if (obj && obj.__esModule) {
        return obj;
      } else {
        var newObj = {};
        if (obj != null) {
          for (var key in obj) {
            if (Object.prototype.hasOwnProperty.call(obj, key)) {
              newObj[key] = obj[key];
            }
          }
        }
        newObj.default = obj;
        return newObj;
      }
    }
    var _tsinterfacechecker = require_dist();
    var t = _interopRequireWildcard(_tsinterfacechecker);
    var Transform = t.union(
      t.lit("jsx"),
      t.lit("typescript"),
      t.lit("flow"),
      t.lit("imports"),
      t.lit("react-hot-loader"),
      t.lit("jest")
    );
    exports.Transform = Transform;
    var SourceMapOptions = t.iface([], {
      compiledFilename: "string"
    });
    exports.SourceMapOptions = SourceMapOptions;
    var Options = t.iface([], {
      transforms: t.array("Transform"),
      disableESTransforms: t.opt("boolean"),
      jsxRuntime: t.opt(t.union(t.lit("classic"), t.lit("automatic"), t.lit("preserve"))),
      production: t.opt("boolean"),
      jsxImportSource: t.opt("string"),
      jsxPragma: t.opt("string"),
      jsxFragmentPragma: t.opt("string"),
      keepUnusedImports: t.opt("boolean"),
      preserveDynamicImport: t.opt("boolean"),
      injectCreateRequireForImportRequire: t.opt("boolean"),
      enableLegacyTypeScriptModuleInterop: t.opt("boolean"),
      enableLegacyBabel5ModuleInterop: t.opt("boolean"),
      sourceMapOptions: t.opt("SourceMapOptions"),
      filePath: t.opt("string")
    });
    exports.Options = Options;
    var exportedTypeSuite = {
      Transform: exports.Transform,
      SourceMapOptions: exports.SourceMapOptions,
      Options: exports.Options
    };
    exports.default = exportedTypeSuite;
  }
});

// node_modules/sucrase/dist/Options.js
var require_Options = __commonJS({
  "node_modules/sucrase/dist/Options.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tsinterfacechecker = require_dist();
    var _Optionsgentypes = require_Options_gen_types();
    var _Optionsgentypes2 = _interopRequireDefault(_Optionsgentypes);
    var { Options: OptionsChecker } = _tsinterfacechecker.createCheckers.call(void 0, _Optionsgentypes2.default);
    function validateOptions(options) {
      OptionsChecker.strictCheck(options);
    }
    exports.validateOptions = validateOptions;
  }
});

// node_modules/sucrase/dist/parser/traverser/lval.js
var require_lval = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/lval.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _flow = require_flow();
    var _typescript = require_typescript();
    var _index = require_tokenizer();
    var _keywords = require_keywords();
    var _types = require_types();
    var _base = require_base();
    var _expression = require_expression();
    var _util = require_util();
    function parseSpread() {
      _index.next.call(void 0);
      _expression.parseMaybeAssign.call(void 0, false);
    }
    exports.parseSpread = parseSpread;
    function parseRest(isBlockScope) {
      _index.next.call(void 0);
      parseBindingAtom(isBlockScope);
    }
    exports.parseRest = parseRest;
    function parseBindingIdentifier(isBlockScope) {
      _expression.parseIdentifier.call(void 0);
      markPriorBindingIdentifier(isBlockScope);
    }
    exports.parseBindingIdentifier = parseBindingIdentifier;
    function parseImportedIdentifier() {
      _expression.parseIdentifier.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ImportDeclaration;
    }
    exports.parseImportedIdentifier = parseImportedIdentifier;
    function markPriorBindingIdentifier(isBlockScope) {
      let identifierRole;
      if (_base.state.scopeDepth === 0) {
        identifierRole = _index.IdentifierRole.TopLevelDeclaration;
      } else if (isBlockScope) {
        identifierRole = _index.IdentifierRole.BlockScopedDeclaration;
      } else {
        identifierRole = _index.IdentifierRole.FunctionScopedDeclaration;
      }
      _base.state.tokens[_base.state.tokens.length - 1].identifierRole = identifierRole;
    }
    exports.markPriorBindingIdentifier = markPriorBindingIdentifier;
    function parseBindingAtom(isBlockScope) {
      switch (_base.state.type) {
        case _types.TokenType._this: {
          const oldIsType = _index.pushTypeContext.call(void 0, 0);
          _index.next.call(void 0);
          _index.popTypeContext.call(void 0, oldIsType);
          return;
        }
        case _types.TokenType._yield:
        case _types.TokenType.name: {
          _base.state.type = _types.TokenType.name;
          parseBindingIdentifier(isBlockScope);
          return;
        }
        case _types.TokenType.bracketL: {
          _index.next.call(void 0);
          parseBindingList(
            _types.TokenType.bracketR,
            isBlockScope,
            true
            /* allowEmpty */
          );
          return;
        }
        case _types.TokenType.braceL:
          _expression.parseObj.call(void 0, true, isBlockScope);
          return;
        default:
          _util.unexpected.call(void 0);
      }
    }
    exports.parseBindingAtom = parseBindingAtom;
    function parseBindingList(close, isBlockScope, allowEmpty = false, allowModifiers = false, contextId = 0) {
      let first = true;
      let hasRemovedComma = false;
      const firstItemTokenIndex = _base.state.tokens.length;
      while (!_index.eat.call(void 0, close) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types.TokenType.comma);
          _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
          if (!hasRemovedComma && _base.state.tokens[firstItemTokenIndex].isType) {
            _base.state.tokens[_base.state.tokens.length - 1].isType = true;
            hasRemovedComma = true;
          }
        }
        if (allowEmpty && _index.match.call(void 0, _types.TokenType.comma)) {
        } else if (_index.eat.call(void 0, close)) {
          break;
        } else if (_index.match.call(void 0, _types.TokenType.ellipsis)) {
          parseRest(isBlockScope);
          parseAssignableListItemTypes();
          _index.eat.call(void 0, _types.TokenType.comma);
          _util.expect.call(void 0, close);
          break;
        } else {
          parseAssignableListItem(allowModifiers, isBlockScope);
        }
      }
    }
    exports.parseBindingList = parseBindingList;
    function parseAssignableListItem(allowModifiers, isBlockScope) {
      if (allowModifiers) {
        _typescript.tsParseModifiers.call(void 0, [
          _keywords.ContextualKeyword._public,
          _keywords.ContextualKeyword._protected,
          _keywords.ContextualKeyword._private,
          _keywords.ContextualKeyword._readonly,
          _keywords.ContextualKeyword._override
        ]);
      }
      parseMaybeDefault(isBlockScope);
      parseAssignableListItemTypes();
      parseMaybeDefault(
        isBlockScope,
        true
        /* leftAlreadyParsed */
      );
    }
    function parseAssignableListItemTypes() {
      if (_base.isFlowEnabled) {
        _flow.flowParseAssignableListItemTypes.call(void 0);
      } else if (_base.isTypeScriptEnabled) {
        _typescript.tsParseAssignableListItemTypes.call(void 0);
      }
    }
    function parseMaybeDefault(isBlockScope, leftAlreadyParsed = false) {
      if (!leftAlreadyParsed) {
        parseBindingAtom(isBlockScope);
      }
      if (!_index.eat.call(void 0, _types.TokenType.eq)) {
        return;
      }
      const eqIndex = _base.state.tokens.length - 1;
      _expression.parseMaybeAssign.call(void 0);
      _base.state.tokens[eqIndex].rhsEndIndex = _base.state.tokens.length;
    }
    exports.parseMaybeDefault = parseMaybeDefault;
  }
});

// node_modules/sucrase/dist/parser/plugins/typescript.js
var require_typescript = __commonJS({
  "node_modules/sucrase/dist/parser/plugins/typescript.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _keywords = require_keywords();
    var _types = require_types();
    var _base = require_base();
    var _expression = require_expression();
    var _lval = require_lval();
    var _statement = require_statement();
    var _util = require_util();
    var _jsx = require_jsx();
    function tsIsIdentifier() {
      return _index.match.call(void 0, _types.TokenType.name);
    }
    function isLiteralPropertyName() {
      return _index.match.call(void 0, _types.TokenType.name) || Boolean(_base.state.type & _types.TokenType.IS_KEYWORD) || _index.match.call(void 0, _types.TokenType.string) || _index.match.call(void 0, _types.TokenType.num) || _index.match.call(void 0, _types.TokenType.bigint) || _index.match.call(void 0, _types.TokenType.decimal);
    }
    function tsNextTokenCanFollowModifier() {
      const snapshot = _base.state.snapshot();
      _index.next.call(void 0);
      const canFollowModifier = (_index.match.call(void 0, _types.TokenType.bracketL) || _index.match.call(void 0, _types.TokenType.braceL) || _index.match.call(void 0, _types.TokenType.star) || _index.match.call(void 0, _types.TokenType.ellipsis) || _index.match.call(void 0, _types.TokenType.hash) || isLiteralPropertyName()) && !_util.hasPrecedingLineBreak.call(void 0);
      if (canFollowModifier) {
        return true;
      } else {
        _base.state.restoreFromSnapshot(snapshot);
        return false;
      }
    }
    function tsParseModifiers(allowedModifiers) {
      while (true) {
        const modifier = tsParseModifier(allowedModifiers);
        if (modifier === null) {
          break;
        }
      }
    }
    exports.tsParseModifiers = tsParseModifiers;
    function tsParseModifier(allowedModifiers) {
      if (!_index.match.call(void 0, _types.TokenType.name)) {
        return null;
      }
      const modifier = _base.state.contextualKeyword;
      if (allowedModifiers.indexOf(modifier) !== -1 && tsNextTokenCanFollowModifier()) {
        switch (modifier) {
          case _keywords.ContextualKeyword._readonly:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._readonly;
            break;
          case _keywords.ContextualKeyword._abstract:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._abstract;
            break;
          case _keywords.ContextualKeyword._static:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._static;
            break;
          case _keywords.ContextualKeyword._public:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._public;
            break;
          case _keywords.ContextualKeyword._private:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._private;
            break;
          case _keywords.ContextualKeyword._protected:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._protected;
            break;
          case _keywords.ContextualKeyword._override:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._override;
            break;
          case _keywords.ContextualKeyword._declare:
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._declare;
            break;
          default:
            break;
        }
        return modifier;
      }
      return null;
    }
    exports.tsParseModifier = tsParseModifier;
    function tsParseEntityName() {
      _expression.parseIdentifier.call(void 0);
      while (_index.eat.call(void 0, _types.TokenType.dot)) {
        _expression.parseIdentifier.call(void 0);
      }
    }
    function tsParseTypeReference() {
      tsParseEntityName();
      if (!_util.hasPrecedingLineBreak.call(void 0) && _index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseTypeArguments();
      }
    }
    function tsParseThisTypePredicate() {
      _index.next.call(void 0);
      tsParseTypeAnnotation();
    }
    function tsParseThisTypeNode() {
      _index.next.call(void 0);
    }
    function tsParseTypeQuery() {
      _util.expect.call(void 0, _types.TokenType._typeof);
      if (_index.match.call(void 0, _types.TokenType._import)) {
        tsParseImportType();
      } else {
        tsParseEntityName();
      }
      if (!_util.hasPrecedingLineBreak.call(void 0) && _index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseTypeArguments();
      }
    }
    function tsParseImportType() {
      _util.expect.call(void 0, _types.TokenType._import);
      _util.expect.call(void 0, _types.TokenType.parenL);
      _util.expect.call(void 0, _types.TokenType.string);
      _util.expect.call(void 0, _types.TokenType.parenR);
      if (_index.eat.call(void 0, _types.TokenType.dot)) {
        tsParseEntityName();
      }
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseTypeArguments();
      }
    }
    function tsParseTypeParameter() {
      _index.eat.call(void 0, _types.TokenType._const);
      const hadIn = _index.eat.call(void 0, _types.TokenType._in);
      const hadOut = _util.eatContextual.call(void 0, _keywords.ContextualKeyword._out);
      _index.eat.call(void 0, _types.TokenType._const);
      if ((hadIn || hadOut) && !_index.match.call(void 0, _types.TokenType.name)) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType.name;
      } else {
        _expression.parseIdentifier.call(void 0);
      }
      if (_index.eat.call(void 0, _types.TokenType._extends)) {
        tsParseType();
      }
      if (_index.eat.call(void 0, _types.TokenType.eq)) {
        tsParseType();
      }
    }
    function tsTryParseTypeParameters() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseTypeParameters();
      }
    }
    exports.tsTryParseTypeParameters = tsTryParseTypeParameters;
    function tsParseTypeParameters() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.typeParameterStart)) {
        _index.next.call(void 0);
      } else {
        _util.unexpected.call(void 0);
      }
      while (!_index.eat.call(void 0, _types.TokenType.greaterThan) && !_base.state.error) {
        tsParseTypeParameter();
        _index.eat.call(void 0, _types.TokenType.comma);
      }
      _index.popTypeContext.call(void 0, oldIsType);
    }
    function tsFillSignature(returnToken) {
      const returnTokenRequired = returnToken === _types.TokenType.arrow;
      tsTryParseTypeParameters();
      _util.expect.call(void 0, _types.TokenType.parenL);
      _base.state.scopeDepth++;
      tsParseBindingListForSignature(
        false
        /* isBlockScope */
      );
      _base.state.scopeDepth--;
      if (returnTokenRequired) {
        tsParseTypeOrTypePredicateAnnotation(returnToken);
      } else if (_index.match.call(void 0, returnToken)) {
        tsParseTypeOrTypePredicateAnnotation(returnToken);
      }
    }
    function tsParseBindingListForSignature(isBlockScope) {
      _lval.parseBindingList.call(void 0, _types.TokenType.parenR, isBlockScope);
    }
    function tsParseTypeMemberSemicolon() {
      if (!_index.eat.call(void 0, _types.TokenType.comma)) {
        _util.semicolon.call(void 0);
      }
    }
    function tsParseSignatureMember() {
      tsFillSignature(_types.TokenType.colon);
      tsParseTypeMemberSemicolon();
    }
    function tsIsUnambiguouslyIndexSignature() {
      const snapshot = _base.state.snapshot();
      _index.next.call(void 0);
      const isIndexSignature = _index.eat.call(void 0, _types.TokenType.name) && _index.match.call(void 0, _types.TokenType.colon);
      _base.state.restoreFromSnapshot(snapshot);
      return isIndexSignature;
    }
    function tsTryParseIndexSignature() {
      if (!(_index.match.call(void 0, _types.TokenType.bracketL) && tsIsUnambiguouslyIndexSignature())) {
        return false;
      }
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, _types.TokenType.bracketL);
      _expression.parseIdentifier.call(void 0);
      tsParseTypeAnnotation();
      _util.expect.call(void 0, _types.TokenType.bracketR);
      tsTryParseTypeAnnotation();
      tsParseTypeMemberSemicolon();
      _index.popTypeContext.call(void 0, oldIsType);
      return true;
    }
    function tsParsePropertyOrMethodSignature(isReadonly) {
      _index.eat.call(void 0, _types.TokenType.question);
      if (!isReadonly && (_index.match.call(void 0, _types.TokenType.parenL) || _index.match.call(void 0, _types.TokenType.lessThan))) {
        tsFillSignature(_types.TokenType.colon);
        tsParseTypeMemberSemicolon();
      } else {
        tsTryParseTypeAnnotation();
        tsParseTypeMemberSemicolon();
      }
    }
    function tsParseTypeMember() {
      if (_index.match.call(void 0, _types.TokenType.parenL) || _index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseSignatureMember();
        return;
      }
      if (_index.match.call(void 0, _types.TokenType._new)) {
        _index.next.call(void 0);
        if (_index.match.call(void 0, _types.TokenType.parenL) || _index.match.call(void 0, _types.TokenType.lessThan)) {
          tsParseSignatureMember();
        } else {
          tsParsePropertyOrMethodSignature(false);
        }
        return;
      }
      const readonly = !!tsParseModifier([_keywords.ContextualKeyword._readonly]);
      const found = tsTryParseIndexSignature();
      if (found) {
        return;
      }
      if ((_util.isContextual.call(void 0, _keywords.ContextualKeyword._get) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._set)) && tsNextTokenCanFollowModifier()) {
      }
      _expression.parsePropertyName.call(
        void 0,
        -1
        /* Types don't need context IDs. */
      );
      tsParsePropertyOrMethodSignature(readonly);
    }
    function tsParseTypeLiteral() {
      tsParseObjectTypeMembers();
    }
    function tsParseObjectTypeMembers() {
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_index.eat.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        tsParseTypeMember();
      }
    }
    function tsLookaheadIsStartOfMappedType() {
      const snapshot = _base.state.snapshot();
      const isStartOfMappedType = tsIsStartOfMappedType();
      _base.state.restoreFromSnapshot(snapshot);
      return isStartOfMappedType;
    }
    function tsIsStartOfMappedType() {
      _index.next.call(void 0);
      if (_index.eat.call(void 0, _types.TokenType.plus) || _index.eat.call(void 0, _types.TokenType.minus)) {
        return _util.isContextual.call(void 0, _keywords.ContextualKeyword._readonly);
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._readonly)) {
        _index.next.call(void 0);
      }
      if (!_index.match.call(void 0, _types.TokenType.bracketL)) {
        return false;
      }
      _index.next.call(void 0);
      if (!tsIsIdentifier()) {
        return false;
      }
      _index.next.call(void 0);
      return _index.match.call(void 0, _types.TokenType._in);
    }
    function tsParseMappedTypeParameter() {
      _expression.parseIdentifier.call(void 0);
      _util.expect.call(void 0, _types.TokenType._in);
      tsParseType();
    }
    function tsParseMappedType() {
      _util.expect.call(void 0, _types.TokenType.braceL);
      if (_index.match.call(void 0, _types.TokenType.plus) || _index.match.call(void 0, _types.TokenType.minus)) {
        _index.next.call(void 0);
        _util.expectContextual.call(void 0, _keywords.ContextualKeyword._readonly);
      } else {
        _util.eatContextual.call(void 0, _keywords.ContextualKeyword._readonly);
      }
      _util.expect.call(void 0, _types.TokenType.bracketL);
      tsParseMappedTypeParameter();
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        tsParseType();
      }
      _util.expect.call(void 0, _types.TokenType.bracketR);
      if (_index.match.call(void 0, _types.TokenType.plus) || _index.match.call(void 0, _types.TokenType.minus)) {
        _index.next.call(void 0);
        _util.expect.call(void 0, _types.TokenType.question);
      } else {
        _index.eat.call(void 0, _types.TokenType.question);
      }
      tsTryParseType();
      _util.semicolon.call(void 0);
      _util.expect.call(void 0, _types.TokenType.braceR);
    }
    function tsParseTupleType() {
      _util.expect.call(void 0, _types.TokenType.bracketL);
      while (!_index.eat.call(void 0, _types.TokenType.bracketR) && !_base.state.error) {
        tsParseTupleElementType();
        _index.eat.call(void 0, _types.TokenType.comma);
      }
    }
    function tsParseTupleElementType() {
      if (_index.eat.call(void 0, _types.TokenType.ellipsis)) {
        tsParseType();
      } else {
        tsParseType();
        _index.eat.call(void 0, _types.TokenType.question);
      }
      if (_index.eat.call(void 0, _types.TokenType.colon)) {
        tsParseType();
      }
    }
    function tsParseParenthesizedType() {
      _util.expect.call(void 0, _types.TokenType.parenL);
      tsParseType();
      _util.expect.call(void 0, _types.TokenType.parenR);
    }
    function tsParseTemplateLiteralType() {
      _index.nextTemplateToken.call(void 0);
      _index.nextTemplateToken.call(void 0);
      while (!_index.match.call(void 0, _types.TokenType.backQuote) && !_base.state.error) {
        _util.expect.call(void 0, _types.TokenType.dollarBraceL);
        tsParseType();
        _index.nextTemplateToken.call(void 0);
        _index.nextTemplateToken.call(void 0);
      }
      _index.next.call(void 0);
    }
    var FunctionType;
    (function(FunctionType2) {
      const TSFunctionType = 0;
      FunctionType2[FunctionType2["TSFunctionType"] = TSFunctionType] = "TSFunctionType";
      const TSConstructorType = TSFunctionType + 1;
      FunctionType2[FunctionType2["TSConstructorType"] = TSConstructorType] = "TSConstructorType";
      const TSAbstractConstructorType = TSConstructorType + 1;
      FunctionType2[FunctionType2["TSAbstractConstructorType"] = TSAbstractConstructorType] = "TSAbstractConstructorType";
    })(FunctionType || (FunctionType = {}));
    function tsParseFunctionOrConstructorType(type) {
      if (type === FunctionType.TSAbstractConstructorType) {
        _util.expectContextual.call(void 0, _keywords.ContextualKeyword._abstract);
      }
      if (type === FunctionType.TSConstructorType || type === FunctionType.TSAbstractConstructorType) {
        _util.expect.call(void 0, _types.TokenType._new);
      }
      const oldInDisallowConditionalTypesContext = _base.state.inDisallowConditionalTypesContext;
      _base.state.inDisallowConditionalTypesContext = false;
      tsFillSignature(_types.TokenType.arrow);
      _base.state.inDisallowConditionalTypesContext = oldInDisallowConditionalTypesContext;
    }
    function tsParseNonArrayType() {
      switch (_base.state.type) {
        case _types.TokenType.name:
          tsParseTypeReference();
          return;
        case _types.TokenType._void:
        case _types.TokenType._null:
          _index.next.call(void 0);
          return;
        case _types.TokenType.string:
        case _types.TokenType.num:
        case _types.TokenType.bigint:
        case _types.TokenType.decimal:
        case _types.TokenType._true:
        case _types.TokenType._false:
          _expression.parseLiteral.call(void 0);
          return;
        case _types.TokenType.minus:
          _index.next.call(void 0);
          _expression.parseLiteral.call(void 0);
          return;
        case _types.TokenType._this: {
          tsParseThisTypeNode();
          if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._is) && !_util.hasPrecedingLineBreak.call(void 0)) {
            tsParseThisTypePredicate();
          }
          return;
        }
        case _types.TokenType._typeof:
          tsParseTypeQuery();
          return;
        case _types.TokenType._import:
          tsParseImportType();
          return;
        case _types.TokenType.braceL:
          if (tsLookaheadIsStartOfMappedType()) {
            tsParseMappedType();
          } else {
            tsParseTypeLiteral();
          }
          return;
        case _types.TokenType.bracketL:
          tsParseTupleType();
          return;
        case _types.TokenType.parenL:
          tsParseParenthesizedType();
          return;
        case _types.TokenType.backQuote:
          tsParseTemplateLiteralType();
          return;
        default:
          if (_base.state.type & _types.TokenType.IS_KEYWORD) {
            _index.next.call(void 0);
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType.name;
            return;
          }
          break;
      }
      _util.unexpected.call(void 0);
    }
    function tsParseArrayTypeOrHigher() {
      tsParseNonArrayType();
      while (!_util.hasPrecedingLineBreak.call(void 0) && _index.eat.call(void 0, _types.TokenType.bracketL)) {
        if (!_index.eat.call(void 0, _types.TokenType.bracketR)) {
          tsParseType();
          _util.expect.call(void 0, _types.TokenType.bracketR);
        }
      }
    }
    function tsParseInferType() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._infer);
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType._extends)) {
        const snapshot = _base.state.snapshot();
        _util.expect.call(void 0, _types.TokenType._extends);
        const oldInDisallowConditionalTypesContext = _base.state.inDisallowConditionalTypesContext;
        _base.state.inDisallowConditionalTypesContext = true;
        tsParseType();
        _base.state.inDisallowConditionalTypesContext = oldInDisallowConditionalTypesContext;
        if (_base.state.error || !_base.state.inDisallowConditionalTypesContext && _index.match.call(void 0, _types.TokenType.question)) {
          _base.state.restoreFromSnapshot(snapshot);
        }
      }
    }
    function tsParseTypeOperatorOrHigher() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._keyof) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._unique) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._readonly)) {
        _index.next.call(void 0);
        tsParseTypeOperatorOrHigher();
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._infer)) {
        tsParseInferType();
      } else {
        const oldInDisallowConditionalTypesContext = _base.state.inDisallowConditionalTypesContext;
        _base.state.inDisallowConditionalTypesContext = false;
        tsParseArrayTypeOrHigher();
        _base.state.inDisallowConditionalTypesContext = oldInDisallowConditionalTypesContext;
      }
    }
    function tsParseIntersectionTypeOrHigher() {
      _index.eat.call(void 0, _types.TokenType.bitwiseAND);
      tsParseTypeOperatorOrHigher();
      if (_index.match.call(void 0, _types.TokenType.bitwiseAND)) {
        while (_index.eat.call(void 0, _types.TokenType.bitwiseAND)) {
          tsParseTypeOperatorOrHigher();
        }
      }
    }
    function tsParseUnionTypeOrHigher() {
      _index.eat.call(void 0, _types.TokenType.bitwiseOR);
      tsParseIntersectionTypeOrHigher();
      if (_index.match.call(void 0, _types.TokenType.bitwiseOR)) {
        while (_index.eat.call(void 0, _types.TokenType.bitwiseOR)) {
          tsParseIntersectionTypeOrHigher();
        }
      }
    }
    function tsIsStartOfFunctionType() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        return true;
      }
      return _index.match.call(void 0, _types.TokenType.parenL) && tsLookaheadIsUnambiguouslyStartOfFunctionType();
    }
    function tsSkipParameterStart() {
      if (_index.match.call(void 0, _types.TokenType.name) || _index.match.call(void 0, _types.TokenType._this)) {
        _index.next.call(void 0);
        return true;
      }
      if (_index.match.call(void 0, _types.TokenType.braceL) || _index.match.call(void 0, _types.TokenType.bracketL)) {
        let depth = 1;
        _index.next.call(void 0);
        while (depth > 0 && !_base.state.error) {
          if (_index.match.call(void 0, _types.TokenType.braceL) || _index.match.call(void 0, _types.TokenType.bracketL)) {
            depth++;
          } else if (_index.match.call(void 0, _types.TokenType.braceR) || _index.match.call(void 0, _types.TokenType.bracketR)) {
            depth--;
          }
          _index.next.call(void 0);
        }
        return true;
      }
      return false;
    }
    function tsLookaheadIsUnambiguouslyStartOfFunctionType() {
      const snapshot = _base.state.snapshot();
      const isUnambiguouslyStartOfFunctionType = tsIsUnambiguouslyStartOfFunctionType();
      _base.state.restoreFromSnapshot(snapshot);
      return isUnambiguouslyStartOfFunctionType;
    }
    function tsIsUnambiguouslyStartOfFunctionType() {
      _index.next.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.parenR) || _index.match.call(void 0, _types.TokenType.ellipsis)) {
        return true;
      }
      if (tsSkipParameterStart()) {
        if (_index.match.call(void 0, _types.TokenType.colon) || _index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.question) || _index.match.call(void 0, _types.TokenType.eq)) {
          return true;
        }
        if (_index.match.call(void 0, _types.TokenType.parenR)) {
          _index.next.call(void 0);
          if (_index.match.call(void 0, _types.TokenType.arrow)) {
            return true;
          }
        }
      }
      return false;
    }
    function tsParseTypeOrTypePredicateAnnotation(returnToken) {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, returnToken);
      const finishedReturn = tsParseTypePredicateOrAssertsPrefix();
      if (!finishedReturn) {
        tsParseType();
      }
      _index.popTypeContext.call(void 0, oldIsType);
    }
    function tsTryParseTypeOrTypePredicateAnnotation() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        tsParseTypeOrTypePredicateAnnotation(_types.TokenType.colon);
      }
    }
    function tsTryParseTypeAnnotation() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        tsParseTypeAnnotation();
      }
    }
    exports.tsTryParseTypeAnnotation = tsTryParseTypeAnnotation;
    function tsTryParseType() {
      if (_index.eat.call(void 0, _types.TokenType.colon)) {
        tsParseType();
      }
    }
    function tsParseTypePredicateOrAssertsPrefix() {
      const snapshot = _base.state.snapshot();
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._asserts)) {
        _index.next.call(void 0);
        if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._is)) {
          tsParseType();
          return true;
        } else if (tsIsIdentifier() || _index.match.call(void 0, _types.TokenType._this)) {
          _index.next.call(void 0);
          if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._is)) {
            tsParseType();
          }
          return true;
        } else {
          _base.state.restoreFromSnapshot(snapshot);
          return false;
        }
      } else if (tsIsIdentifier() || _index.match.call(void 0, _types.TokenType._this)) {
        _index.next.call(void 0);
        if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._is) && !_util.hasPrecedingLineBreak.call(void 0)) {
          _index.next.call(void 0);
          tsParseType();
          return true;
        } else {
          _base.state.restoreFromSnapshot(snapshot);
          return false;
        }
      }
      return false;
    }
    function tsParseTypeAnnotation() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, _types.TokenType.colon);
      tsParseType();
      _index.popTypeContext.call(void 0, oldIsType);
    }
    exports.tsParseTypeAnnotation = tsParseTypeAnnotation;
    function tsParseType() {
      tsParseNonConditionalType();
      if (_base.state.inDisallowConditionalTypesContext || _util.hasPrecedingLineBreak.call(void 0) || !_index.eat.call(void 0, _types.TokenType._extends)) {
        return;
      }
      const oldInDisallowConditionalTypesContext = _base.state.inDisallowConditionalTypesContext;
      _base.state.inDisallowConditionalTypesContext = true;
      tsParseNonConditionalType();
      _base.state.inDisallowConditionalTypesContext = oldInDisallowConditionalTypesContext;
      _util.expect.call(void 0, _types.TokenType.question);
      tsParseType();
      _util.expect.call(void 0, _types.TokenType.colon);
      tsParseType();
    }
    exports.tsParseType = tsParseType;
    function isAbstractConstructorSignature() {
      return _util.isContextual.call(void 0, _keywords.ContextualKeyword._abstract) && _index.lookaheadType.call(void 0) === _types.TokenType._new;
    }
    function tsParseNonConditionalType() {
      if (tsIsStartOfFunctionType()) {
        tsParseFunctionOrConstructorType(FunctionType.TSFunctionType);
        return;
      }
      if (_index.match.call(void 0, _types.TokenType._new)) {
        tsParseFunctionOrConstructorType(FunctionType.TSConstructorType);
        return;
      } else if (isAbstractConstructorSignature()) {
        tsParseFunctionOrConstructorType(FunctionType.TSAbstractConstructorType);
        return;
      }
      tsParseUnionTypeOrHigher();
    }
    exports.tsParseNonConditionalType = tsParseNonConditionalType;
    function tsParseTypeAssertion() {
      const oldIsType = _index.pushTypeContext.call(void 0, 1);
      tsParseType();
      _util.expect.call(void 0, _types.TokenType.greaterThan);
      _index.popTypeContext.call(void 0, oldIsType);
      _expression.parseMaybeUnary.call(void 0);
    }
    exports.tsParseTypeAssertion = tsParseTypeAssertion;
    function tsTryParseJSXTypeArgument() {
      if (_index.eat.call(void 0, _types.TokenType.jsxTagStart)) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType.typeParameterStart;
        const oldIsType = _index.pushTypeContext.call(void 0, 1);
        while (!_index.match.call(void 0, _types.TokenType.greaterThan) && !_base.state.error) {
          tsParseType();
          _index.eat.call(void 0, _types.TokenType.comma);
        }
        _jsx.nextJSXTagToken.call(void 0);
        _index.popTypeContext.call(void 0, oldIsType);
      }
    }
    exports.tsTryParseJSXTypeArgument = tsTryParseJSXTypeArgument;
    function tsParseHeritageClause() {
      while (!_index.match.call(void 0, _types.TokenType.braceL) && !_base.state.error) {
        tsParseExpressionWithTypeArguments();
        _index.eat.call(void 0, _types.TokenType.comma);
      }
    }
    function tsParseExpressionWithTypeArguments() {
      tsParseEntityName();
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        tsParseTypeArguments();
      }
    }
    function tsParseInterfaceDeclaration() {
      _lval.parseBindingIdentifier.call(void 0, false);
      tsTryParseTypeParameters();
      if (_index.eat.call(void 0, _types.TokenType._extends)) {
        tsParseHeritageClause();
      }
      tsParseObjectTypeMembers();
    }
    function tsParseTypeAliasDeclaration() {
      _lval.parseBindingIdentifier.call(void 0, false);
      tsTryParseTypeParameters();
      _util.expect.call(void 0, _types.TokenType.eq);
      tsParseType();
      _util.semicolon.call(void 0);
    }
    function tsParseEnumMember() {
      if (_index.match.call(void 0, _types.TokenType.string)) {
        _expression.parseLiteral.call(void 0);
      } else {
        _expression.parseIdentifier.call(void 0);
      }
      if (_index.eat.call(void 0, _types.TokenType.eq)) {
        const eqIndex = _base.state.tokens.length - 1;
        _expression.parseMaybeAssign.call(void 0);
        _base.state.tokens[eqIndex].rhsEndIndex = _base.state.tokens.length;
      }
    }
    function tsParseEnumDeclaration() {
      _lval.parseBindingIdentifier.call(void 0, false);
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_index.eat.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        tsParseEnumMember();
        _index.eat.call(void 0, _types.TokenType.comma);
      }
    }
    function tsParseModuleBlock() {
      _util.expect.call(void 0, _types.TokenType.braceL);
      _statement.parseBlockBody.call(
        void 0,
        /* end */
        _types.TokenType.braceR
      );
    }
    function tsParseModuleOrNamespaceDeclaration() {
      _lval.parseBindingIdentifier.call(void 0, false);
      if (_index.eat.call(void 0, _types.TokenType.dot)) {
        tsParseModuleOrNamespaceDeclaration();
      } else {
        tsParseModuleBlock();
      }
    }
    function tsParseAmbientExternalModuleDeclaration() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._global)) {
        _expression.parseIdentifier.call(void 0);
      } else if (_index.match.call(void 0, _types.TokenType.string)) {
        _expression.parseExprAtom.call(void 0);
      } else {
        _util.unexpected.call(void 0);
      }
      if (_index.match.call(void 0, _types.TokenType.braceL)) {
        tsParseModuleBlock();
      } else {
        _util.semicolon.call(void 0);
      }
    }
    function tsParseImportEqualsDeclaration() {
      _lval.parseImportedIdentifier.call(void 0);
      _util.expect.call(void 0, _types.TokenType.eq);
      tsParseModuleReference();
      _util.semicolon.call(void 0);
    }
    exports.tsParseImportEqualsDeclaration = tsParseImportEqualsDeclaration;
    function tsIsExternalModuleReference() {
      return _util.isContextual.call(void 0, _keywords.ContextualKeyword._require) && _index.lookaheadType.call(void 0) === _types.TokenType.parenL;
    }
    function tsParseModuleReference() {
      if (tsIsExternalModuleReference()) {
        tsParseExternalModuleReference();
      } else {
        tsParseEntityName();
      }
    }
    function tsParseExternalModuleReference() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._require);
      _util.expect.call(void 0, _types.TokenType.parenL);
      if (!_index.match.call(void 0, _types.TokenType.string)) {
        _util.unexpected.call(void 0);
      }
      _expression.parseLiteral.call(void 0);
      _util.expect.call(void 0, _types.TokenType.parenR);
    }
    function tsTryParseDeclare() {
      if (_util.isLineTerminator.call(void 0)) {
        return false;
      }
      switch (_base.state.type) {
        case _types.TokenType._function: {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          _index.next.call(void 0);
          const functionStart = _base.state.start;
          _statement.parseFunction.call(
            void 0,
            functionStart,
            /* isStatement */
            true
          );
          _index.popTypeContext.call(void 0, oldIsType);
          return true;
        }
        case _types.TokenType._class: {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          _statement.parseClass.call(
            void 0,
            /* isStatement */
            true,
            /* optionalId */
            false
          );
          _index.popTypeContext.call(void 0, oldIsType);
          return true;
        }
        case _types.TokenType._const: {
          if (_index.match.call(void 0, _types.TokenType._const) && _util.isLookaheadContextual.call(void 0, _keywords.ContextualKeyword._enum)) {
            const oldIsType = _index.pushTypeContext.call(void 0, 1);
            _util.expect.call(void 0, _types.TokenType._const);
            _util.expectContextual.call(void 0, _keywords.ContextualKeyword._enum);
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._enum;
            tsParseEnumDeclaration();
            _index.popTypeContext.call(void 0, oldIsType);
            return true;
          }
        }
        // falls through
        case _types.TokenType._var:
        case _types.TokenType._let: {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          _statement.parseVarStatement.call(void 0, _base.state.type !== _types.TokenType._var);
          _index.popTypeContext.call(void 0, oldIsType);
          return true;
        }
        case _types.TokenType.name: {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          const contextualKeyword = _base.state.contextualKeyword;
          let matched = false;
          if (contextualKeyword === _keywords.ContextualKeyword._global) {
            tsParseAmbientExternalModuleDeclaration();
            matched = true;
          } else {
            matched = tsParseDeclaration(
              contextualKeyword,
              /* isBeforeToken */
              true
            );
          }
          _index.popTypeContext.call(void 0, oldIsType);
          return matched;
        }
        default:
          return false;
      }
    }
    function tsTryParseExportDeclaration() {
      return tsParseDeclaration(
        _base.state.contextualKeyword,
        /* isBeforeToken */
        true
      );
    }
    function tsParseExpressionStatement(contextualKeyword) {
      switch (contextualKeyword) {
        case _keywords.ContextualKeyword._declare: {
          const declareTokenIndex = _base.state.tokens.length - 1;
          const matched = tsTryParseDeclare();
          if (matched) {
            _base.state.tokens[declareTokenIndex].type = _types.TokenType._declare;
            return true;
          }
          break;
        }
        case _keywords.ContextualKeyword._global:
          if (_index.match.call(void 0, _types.TokenType.braceL)) {
            tsParseModuleBlock();
            return true;
          }
          break;
        default:
          return tsParseDeclaration(
            contextualKeyword,
            /* isBeforeToken */
            false
          );
      }
      return false;
    }
    function tsParseDeclaration(contextualKeyword, isBeforeToken) {
      switch (contextualKeyword) {
        case _keywords.ContextualKeyword._abstract:
          if (tsCheckLineTerminator(isBeforeToken) && _index.match.call(void 0, _types.TokenType._class)) {
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._abstract;
            _statement.parseClass.call(
              void 0,
              /* isStatement */
              true,
              /* optionalId */
              false
            );
            return true;
          }
          break;
        case _keywords.ContextualKeyword._enum:
          if (tsCheckLineTerminator(isBeforeToken) && _index.match.call(void 0, _types.TokenType.name)) {
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._enum;
            tsParseEnumDeclaration();
            return true;
          }
          break;
        case _keywords.ContextualKeyword._interface:
          if (tsCheckLineTerminator(isBeforeToken) && _index.match.call(void 0, _types.TokenType.name)) {
            const oldIsType = _index.pushTypeContext.call(void 0, isBeforeToken ? 2 : 1);
            tsParseInterfaceDeclaration();
            _index.popTypeContext.call(void 0, oldIsType);
            return true;
          }
          break;
        case _keywords.ContextualKeyword._module:
          if (tsCheckLineTerminator(isBeforeToken)) {
            if (_index.match.call(void 0, _types.TokenType.string)) {
              const oldIsType = _index.pushTypeContext.call(void 0, isBeforeToken ? 2 : 1);
              tsParseAmbientExternalModuleDeclaration();
              _index.popTypeContext.call(void 0, oldIsType);
              return true;
            } else if (_index.match.call(void 0, _types.TokenType.name)) {
              const oldIsType = _index.pushTypeContext.call(void 0, isBeforeToken ? 2 : 1);
              tsParseModuleOrNamespaceDeclaration();
              _index.popTypeContext.call(void 0, oldIsType);
              return true;
            }
          }
          break;
        case _keywords.ContextualKeyword._namespace:
          if (tsCheckLineTerminator(isBeforeToken) && _index.match.call(void 0, _types.TokenType.name)) {
            const oldIsType = _index.pushTypeContext.call(void 0, isBeforeToken ? 2 : 1);
            tsParseModuleOrNamespaceDeclaration();
            _index.popTypeContext.call(void 0, oldIsType);
            return true;
          }
          break;
        case _keywords.ContextualKeyword._type:
          if (tsCheckLineTerminator(isBeforeToken) && _index.match.call(void 0, _types.TokenType.name)) {
            const oldIsType = _index.pushTypeContext.call(void 0, isBeforeToken ? 2 : 1);
            tsParseTypeAliasDeclaration();
            _index.popTypeContext.call(void 0, oldIsType);
            return true;
          }
          break;
        default:
          break;
      }
      return false;
    }
    function tsCheckLineTerminator(isBeforeToken) {
      if (isBeforeToken) {
        _index.next.call(void 0);
        return true;
      } else {
        return !_util.isLineTerminator.call(void 0);
      }
    }
    function tsTryParseGenericAsyncArrowFunction() {
      const snapshot = _base.state.snapshot();
      tsParseTypeParameters();
      _statement.parseFunctionParams.call(void 0);
      tsTryParseTypeOrTypePredicateAnnotation();
      _util.expect.call(void 0, _types.TokenType.arrow);
      if (_base.state.error) {
        _base.state.restoreFromSnapshot(snapshot);
        return false;
      }
      _expression.parseFunctionBody.call(void 0, true);
      return true;
    }
    function tsParseTypeArgumentsWithPossibleBitshift() {
      if (_base.state.type === _types.TokenType.bitShiftL) {
        _base.state.pos -= 1;
        _index.finishToken.call(void 0, _types.TokenType.lessThan);
      }
      tsParseTypeArguments();
    }
    function tsParseTypeArguments() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, _types.TokenType.lessThan);
      while (!_index.match.call(void 0, _types.TokenType.greaterThan) && !_base.state.error) {
        tsParseType();
        _index.eat.call(void 0, _types.TokenType.comma);
      }
      if (!oldIsType) {
        _index.popTypeContext.call(void 0, oldIsType);
        _index.rescan_gt.call(void 0);
        _util.expect.call(void 0, _types.TokenType.greaterThan);
        _base.state.tokens[_base.state.tokens.length - 1].isType = true;
      } else {
        _util.expect.call(void 0, _types.TokenType.greaterThan);
        _index.popTypeContext.call(void 0, oldIsType);
      }
    }
    function tsIsDeclarationStart() {
      if (_index.match.call(void 0, _types.TokenType.name)) {
        switch (_base.state.contextualKeyword) {
          case _keywords.ContextualKeyword._abstract:
          case _keywords.ContextualKeyword._declare:
          case _keywords.ContextualKeyword._enum:
          case _keywords.ContextualKeyword._interface:
          case _keywords.ContextualKeyword._module:
          case _keywords.ContextualKeyword._namespace:
          case _keywords.ContextualKeyword._type:
            return true;
          default:
            break;
        }
      }
      return false;
    }
    exports.tsIsDeclarationStart = tsIsDeclarationStart;
    function tsParseFunctionBodyAndFinish(functionStart, funcContextId) {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        tsParseTypeOrTypePredicateAnnotation(_types.TokenType.colon);
      }
      if (!_index.match.call(void 0, _types.TokenType.braceL) && _util.isLineTerminator.call(void 0)) {
        let i = _base.state.tokens.length - 1;
        while (i >= 0 && (_base.state.tokens[i].start >= functionStart || _base.state.tokens[i].type === _types.TokenType._default || _base.state.tokens[i].type === _types.TokenType._export)) {
          _base.state.tokens[i].isType = true;
          i--;
        }
        return;
      }
      _expression.parseFunctionBody.call(void 0, false, funcContextId);
    }
    exports.tsParseFunctionBodyAndFinish = tsParseFunctionBodyAndFinish;
    function tsParseSubscript(startTokenIndex, noCalls, stopState) {
      if (!_util.hasPrecedingLineBreak.call(void 0) && _index.eat.call(void 0, _types.TokenType.bang)) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType.nonNullAssertion;
        return;
      }
      if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.bitShiftL)) {
        const snapshot = _base.state.snapshot();
        if (!noCalls && _expression.atPossibleAsync.call(void 0)) {
          const asyncArrowFn = tsTryParseGenericAsyncArrowFunction();
          if (asyncArrowFn) {
            return;
          }
        }
        tsParseTypeArgumentsWithPossibleBitshift();
        if (!noCalls && _index.eat.call(void 0, _types.TokenType.parenL)) {
          _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
          _expression.parseCallExpressionArguments.call(void 0);
        } else if (_index.match.call(void 0, _types.TokenType.backQuote)) {
          _expression.parseTemplate.call(void 0);
        } else if (
          // The remaining possible case is an instantiation expression, e.g.
          // Array<number> . Check for a few cases that would disqualify it and
          // cause us to bail out.
          // a<b>>c is not (a<b>)>c, but a<(b>>c)
          _base.state.type === _types.TokenType.greaterThan || // a<b>c is (a<b)>c
          _base.state.type !== _types.TokenType.parenL && Boolean(_base.state.type & _types.TokenType.IS_EXPRESSION_START) && !_util.hasPrecedingLineBreak.call(void 0)
        ) {
          _util.unexpected.call(void 0);
        }
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
        } else {
          return;
        }
      } else if (!noCalls && _index.match.call(void 0, _types.TokenType.questionDot) && _index.lookaheadType.call(void 0) === _types.TokenType.lessThan) {
        _index.next.call(void 0);
        _base.state.tokens[startTokenIndex].isOptionalChainStart = true;
        _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
        tsParseTypeArguments();
        _util.expect.call(void 0, _types.TokenType.parenL);
        _expression.parseCallExpressionArguments.call(void 0);
      }
      _expression.baseParseSubscript.call(void 0, startTokenIndex, noCalls, stopState);
    }
    exports.tsParseSubscript = tsParseSubscript;
    function tsTryParseExport() {
      if (_index.eat.call(void 0, _types.TokenType._import)) {
        if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._type) && _index.lookaheadType.call(void 0) !== _types.TokenType.eq) {
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._type);
        }
        tsParseImportEqualsDeclaration();
        return true;
      } else if (_index.eat.call(void 0, _types.TokenType.eq)) {
        _expression.parseExpression.call(void 0);
        _util.semicolon.call(void 0);
        return true;
      } else if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        _util.expectContextual.call(void 0, _keywords.ContextualKeyword._namespace);
        _expression.parseIdentifier.call(void 0);
        _util.semicolon.call(void 0);
        return true;
      } else {
        if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._type)) {
          const nextType = _index.lookaheadType.call(void 0);
          if (nextType === _types.TokenType.braceL || nextType === _types.TokenType.star) {
            _index.next.call(void 0);
          }
        }
        return false;
      }
    }
    exports.tsTryParseExport = tsTryParseExport;
    function tsParseImportSpecifier() {
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ImportDeclaration;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ImportDeclaration;
        _base.state.tokens[_base.state.tokens.length - 2].isType = true;
        _base.state.tokens[_base.state.tokens.length - 1].isType = true;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 3].identifierRole = _index.IdentifierRole.ImportAccess;
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ImportDeclaration;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 3].identifierRole = _index.IdentifierRole.ImportAccess;
      _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ImportDeclaration;
      _base.state.tokens[_base.state.tokens.length - 4].isType = true;
      _base.state.tokens[_base.state.tokens.length - 3].isType = true;
      _base.state.tokens[_base.state.tokens.length - 2].isType = true;
      _base.state.tokens[_base.state.tokens.length - 1].isType = true;
    }
    exports.tsParseImportSpecifier = tsParseImportSpecifier;
    function tsParseExportSpecifier() {
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ExportAccess;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index.IdentifierRole.ExportAccess;
        _base.state.tokens[_base.state.tokens.length - 2].isType = true;
        _base.state.tokens[_base.state.tokens.length - 1].isType = true;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.braceR)) {
        _base.state.tokens[_base.state.tokens.length - 3].identifierRole = _index.IdentifierRole.ExportAccess;
        return;
      }
      _expression.parseIdentifier.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 3].identifierRole = _index.IdentifierRole.ExportAccess;
      _base.state.tokens[_base.state.tokens.length - 4].isType = true;
      _base.state.tokens[_base.state.tokens.length - 3].isType = true;
      _base.state.tokens[_base.state.tokens.length - 2].isType = true;
      _base.state.tokens[_base.state.tokens.length - 1].isType = true;
    }
    exports.tsParseExportSpecifier = tsParseExportSpecifier;
    function tsTryParseExportDefaultExpression() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._abstract) && _index.lookaheadType.call(void 0) === _types.TokenType._class) {
        _base.state.type = _types.TokenType._abstract;
        _index.next.call(void 0);
        _statement.parseClass.call(void 0, true, true);
        return true;
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._interface)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 2);
        tsParseDeclaration(_keywords.ContextualKeyword._interface, true);
        _index.popTypeContext.call(void 0, oldIsType);
        return true;
      }
      return false;
    }
    exports.tsTryParseExportDefaultExpression = tsTryParseExportDefaultExpression;
    function tsTryParseStatementContent() {
      if (_base.state.type === _types.TokenType._const) {
        const ahead = _index.lookaheadTypeAndKeyword.call(void 0);
        if (ahead.type === _types.TokenType.name && ahead.contextualKeyword === _keywords.ContextualKeyword._enum) {
          _util.expect.call(void 0, _types.TokenType._const);
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._enum);
          _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._enum;
          tsParseEnumDeclaration();
          return true;
        }
      }
      return false;
    }
    exports.tsTryParseStatementContent = tsTryParseStatementContent;
    function tsTryParseClassMemberWithIsStatic(isStatic) {
      const memberStartIndexAfterStatic = _base.state.tokens.length;
      tsParseModifiers([
        _keywords.ContextualKeyword._abstract,
        _keywords.ContextualKeyword._readonly,
        _keywords.ContextualKeyword._declare,
        _keywords.ContextualKeyword._static,
        _keywords.ContextualKeyword._override
      ]);
      const modifiersEndIndex = _base.state.tokens.length;
      const found = tsTryParseIndexSignature();
      if (found) {
        const memberStartIndex = isStatic ? memberStartIndexAfterStatic - 1 : memberStartIndexAfterStatic;
        for (let i = memberStartIndex; i < modifiersEndIndex; i++) {
          _base.state.tokens[i].isType = true;
        }
        return true;
      }
      return false;
    }
    exports.tsTryParseClassMemberWithIsStatic = tsTryParseClassMemberWithIsStatic;
    function tsParseIdentifierStatement(contextualKeyword) {
      const matched = tsParseExpressionStatement(contextualKeyword);
      if (!matched) {
        _util.semicolon.call(void 0);
      }
    }
    exports.tsParseIdentifierStatement = tsParseIdentifierStatement;
    function tsParseExportDeclaration() {
      const isDeclare = _util.eatContextual.call(void 0, _keywords.ContextualKeyword._declare);
      if (isDeclare) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._declare;
      }
      let matchedDeclaration = false;
      if (_index.match.call(void 0, _types.TokenType.name)) {
        if (isDeclare) {
          const oldIsType = _index.pushTypeContext.call(void 0, 2);
          matchedDeclaration = tsTryParseExportDeclaration();
          _index.popTypeContext.call(void 0, oldIsType);
        } else {
          matchedDeclaration = tsTryParseExportDeclaration();
        }
      }
      if (!matchedDeclaration) {
        if (isDeclare) {
          const oldIsType = _index.pushTypeContext.call(void 0, 2);
          _statement.parseStatement.call(void 0, true);
          _index.popTypeContext.call(void 0, oldIsType);
        } else {
          _statement.parseStatement.call(void 0, true);
        }
      }
    }
    exports.tsParseExportDeclaration = tsParseExportDeclaration;
    function tsAfterParseClassSuper(hasSuper) {
      if (hasSuper && (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.bitShiftL))) {
        tsParseTypeArgumentsWithPossibleBitshift();
      }
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._implements)) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._implements;
        const oldIsType = _index.pushTypeContext.call(void 0, 1);
        tsParseHeritageClause();
        _index.popTypeContext.call(void 0, oldIsType);
      }
    }
    exports.tsAfterParseClassSuper = tsAfterParseClassSuper;
    function tsStartParseObjPropValue() {
      tsTryParseTypeParameters();
    }
    exports.tsStartParseObjPropValue = tsStartParseObjPropValue;
    function tsStartParseFunctionParams() {
      tsTryParseTypeParameters();
    }
    exports.tsStartParseFunctionParams = tsStartParseFunctionParams;
    function tsAfterParseVarHead() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      if (!_util.hasPrecedingLineBreak.call(void 0)) {
        _index.eat.call(void 0, _types.TokenType.bang);
      }
      tsTryParseTypeAnnotation();
      _index.popTypeContext.call(void 0, oldIsType);
    }
    exports.tsAfterParseVarHead = tsAfterParseVarHead;
    function tsStartParseAsyncArrowFromCallExpression() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        tsParseTypeAnnotation();
      }
    }
    exports.tsStartParseAsyncArrowFromCallExpression = tsStartParseAsyncArrowFromCallExpression;
    function tsParseMaybeAssign(noIn, isWithinParens) {
      if (_base.isJSXEnabled) {
        return tsParseMaybeAssignWithJSX(noIn, isWithinParens);
      } else {
        return tsParseMaybeAssignWithoutJSX(noIn, isWithinParens);
      }
    }
    exports.tsParseMaybeAssign = tsParseMaybeAssign;
    function tsParseMaybeAssignWithJSX(noIn, isWithinParens) {
      if (!_index.match.call(void 0, _types.TokenType.lessThan)) {
        return _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
      }
      const snapshot = _base.state.snapshot();
      let wasArrow = _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
      if (_base.state.error) {
        _base.state.restoreFromSnapshot(snapshot);
      } else {
        return wasArrow;
      }
      _base.state.type = _types.TokenType.typeParameterStart;
      tsParseTypeParameters();
      wasArrow = _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
      if (!wasArrow) {
        _util.unexpected.call(void 0);
      }
      return wasArrow;
    }
    exports.tsParseMaybeAssignWithJSX = tsParseMaybeAssignWithJSX;
    function tsParseMaybeAssignWithoutJSX(noIn, isWithinParens) {
      if (!_index.match.call(void 0, _types.TokenType.lessThan)) {
        return _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
      }
      const snapshot = _base.state.snapshot();
      tsParseTypeParameters();
      const wasArrow = _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
      if (!wasArrow) {
        _util.unexpected.call(void 0);
      }
      if (_base.state.error) {
        _base.state.restoreFromSnapshot(snapshot);
      } else {
        return wasArrow;
      }
      return _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
    }
    exports.tsParseMaybeAssignWithoutJSX = tsParseMaybeAssignWithoutJSX;
    function tsParseArrow() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        const snapshot = _base.state.snapshot();
        tsParseTypeOrTypePredicateAnnotation(_types.TokenType.colon);
        if (_util.canInsertSemicolon.call(void 0)) _util.unexpected.call(void 0);
        if (!_index.match.call(void 0, _types.TokenType.arrow)) _util.unexpected.call(void 0);
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
        }
      }
      return _index.eat.call(void 0, _types.TokenType.arrow);
    }
    exports.tsParseArrow = tsParseArrow;
    function tsParseAssignableListItemTypes() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _index.eat.call(void 0, _types.TokenType.question);
      tsTryParseTypeAnnotation();
      _index.popTypeContext.call(void 0, oldIsType);
    }
    exports.tsParseAssignableListItemTypes = tsParseAssignableListItemTypes;
    function tsParseMaybeDecoratorArguments() {
      if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.bitShiftL)) {
        tsParseTypeArgumentsWithPossibleBitshift();
      }
      _statement.baseParseMaybeDecoratorArguments.call(void 0);
    }
    exports.tsParseMaybeDecoratorArguments = tsParseMaybeDecoratorArguments;
  }
});

// node_modules/sucrase/dist/parser/plugins/jsx/index.js
var require_jsx = __commonJS({
  "node_modules/sucrase/dist/parser/plugins/jsx/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _types = require_types();
    var _base = require_base();
    var _expression = require_expression();
    var _util = require_util();
    var _charcodes = require_charcodes();
    var _identifier = require_identifier();
    var _typescript = require_typescript();
    function jsxReadToken() {
      let sawNewline = false;
      let sawNonWhitespace = false;
      while (true) {
        if (_base.state.pos >= _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated JSX contents");
          return;
        }
        const ch = _base.input.charCodeAt(_base.state.pos);
        if (ch === _charcodes.charCodes.lessThan || ch === _charcodes.charCodes.leftCurlyBrace) {
          if (_base.state.pos === _base.state.start) {
            if (ch === _charcodes.charCodes.lessThan) {
              _base.state.pos++;
              _index.finishToken.call(void 0, _types.TokenType.jsxTagStart);
              return;
            }
            _index.getTokenFromCode.call(void 0, ch);
            return;
          }
          if (sawNewline && !sawNonWhitespace) {
            _index.finishToken.call(void 0, _types.TokenType.jsxEmptyText);
          } else {
            _index.finishToken.call(void 0, _types.TokenType.jsxText);
          }
          return;
        }
        if (ch === _charcodes.charCodes.lineFeed) {
          sawNewline = true;
        } else if (ch !== _charcodes.charCodes.space && ch !== _charcodes.charCodes.carriageReturn && ch !== _charcodes.charCodes.tab) {
          sawNonWhitespace = true;
        }
        _base.state.pos++;
      }
    }
    function jsxReadString(quote) {
      _base.state.pos++;
      for (; ; ) {
        if (_base.state.pos >= _base.input.length) {
          _util.unexpected.call(void 0, "Unterminated string constant");
          return;
        }
        const ch = _base.input.charCodeAt(_base.state.pos);
        if (ch === quote) {
          _base.state.pos++;
          break;
        }
        _base.state.pos++;
      }
      _index.finishToken.call(void 0, _types.TokenType.string);
    }
    function jsxReadWord() {
      let ch;
      do {
        if (_base.state.pos > _base.input.length) {
          _util.unexpected.call(void 0, "Unexpectedly reached the end of input.");
          return;
        }
        ch = _base.input.charCodeAt(++_base.state.pos);
      } while (_identifier.IS_IDENTIFIER_CHAR[ch] || ch === _charcodes.charCodes.dash);
      _index.finishToken.call(void 0, _types.TokenType.jsxName);
    }
    function jsxParseIdentifier() {
      nextJSXTagToken();
    }
    function jsxParseNamespacedName(identifierRole) {
      jsxParseIdentifier();
      if (!_index.eat.call(void 0, _types.TokenType.colon)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = identifierRole;
        return;
      }
      jsxParseIdentifier();
    }
    function jsxParseElementName() {
      const firstTokenIndex = _base.state.tokens.length;
      jsxParseNamespacedName(_index.IdentifierRole.Access);
      let hadDot = false;
      while (_index.match.call(void 0, _types.TokenType.dot)) {
        hadDot = true;
        nextJSXTagToken();
        jsxParseIdentifier();
      }
      if (!hadDot) {
        const firstToken = _base.state.tokens[firstTokenIndex];
        const firstChar = _base.input.charCodeAt(firstToken.start);
        if (firstChar >= _charcodes.charCodes.lowercaseA && firstChar <= _charcodes.charCodes.lowercaseZ) {
          firstToken.identifierRole = null;
        }
      }
    }
    function jsxParseAttributeValue() {
      switch (_base.state.type) {
        case _types.TokenType.braceL:
          _index.next.call(void 0);
          _expression.parseExpression.call(void 0);
          nextJSXTagToken();
          return;
        case _types.TokenType.jsxTagStart:
          jsxParseElement();
          nextJSXTagToken();
          return;
        case _types.TokenType.string:
          nextJSXTagToken();
          return;
        default:
          _util.unexpected.call(void 0, "JSX value should be either an expression or a quoted JSX text");
      }
    }
    function jsxParseSpreadChild() {
      _util.expect.call(void 0, _types.TokenType.ellipsis);
      _expression.parseExpression.call(void 0);
    }
    function jsxParseOpeningElement(initialTokenIndex) {
      if (_index.match.call(void 0, _types.TokenType.jsxTagEnd)) {
        return false;
      }
      jsxParseElementName();
      if (_base.isTypeScriptEnabled) {
        _typescript.tsTryParseJSXTypeArgument.call(void 0);
      }
      let hasSeenPropSpread = false;
      while (!_index.match.call(void 0, _types.TokenType.slash) && !_index.match.call(void 0, _types.TokenType.jsxTagEnd) && !_base.state.error) {
        if (_index.eat.call(void 0, _types.TokenType.braceL)) {
          hasSeenPropSpread = true;
          _util.expect.call(void 0, _types.TokenType.ellipsis);
          _expression.parseMaybeAssign.call(void 0);
          nextJSXTagToken();
          continue;
        }
        if (hasSeenPropSpread && _base.state.end - _base.state.start === 3 && _base.input.charCodeAt(_base.state.start) === _charcodes.charCodes.lowercaseK && _base.input.charCodeAt(_base.state.start + 1) === _charcodes.charCodes.lowercaseE && _base.input.charCodeAt(_base.state.start + 2) === _charcodes.charCodes.lowercaseY) {
          _base.state.tokens[initialTokenIndex].jsxRole = _index.JSXRole.KeyAfterPropSpread;
        }
        jsxParseNamespacedName(_index.IdentifierRole.ObjectKey);
        if (_index.match.call(void 0, _types.TokenType.eq)) {
          nextJSXTagToken();
          jsxParseAttributeValue();
        }
      }
      const isSelfClosing = _index.match.call(void 0, _types.TokenType.slash);
      if (isSelfClosing) {
        nextJSXTagToken();
      }
      return isSelfClosing;
    }
    function jsxParseClosingElement() {
      if (_index.match.call(void 0, _types.TokenType.jsxTagEnd)) {
        return;
      }
      jsxParseElementName();
    }
    function jsxParseElementAt() {
      const initialTokenIndex = _base.state.tokens.length - 1;
      _base.state.tokens[initialTokenIndex].jsxRole = _index.JSXRole.NoChildren;
      let numExplicitChildren = 0;
      const isSelfClosing = jsxParseOpeningElement(initialTokenIndex);
      if (!isSelfClosing) {
        nextJSXExprToken();
        while (true) {
          switch (_base.state.type) {
            case _types.TokenType.jsxTagStart:
              nextJSXTagToken();
              if (_index.match.call(void 0, _types.TokenType.slash)) {
                nextJSXTagToken();
                jsxParseClosingElement();
                if (_base.state.tokens[initialTokenIndex].jsxRole !== _index.JSXRole.KeyAfterPropSpread) {
                  if (numExplicitChildren === 1) {
                    _base.state.tokens[initialTokenIndex].jsxRole = _index.JSXRole.OneChild;
                  } else if (numExplicitChildren > 1) {
                    _base.state.tokens[initialTokenIndex].jsxRole = _index.JSXRole.StaticChildren;
                  }
                }
                return;
              }
              numExplicitChildren++;
              jsxParseElementAt();
              nextJSXExprToken();
              break;
            case _types.TokenType.jsxText:
              numExplicitChildren++;
              nextJSXExprToken();
              break;
            case _types.TokenType.jsxEmptyText:
              nextJSXExprToken();
              break;
            case _types.TokenType.braceL:
              _index.next.call(void 0);
              if (_index.match.call(void 0, _types.TokenType.ellipsis)) {
                jsxParseSpreadChild();
                nextJSXExprToken();
                numExplicitChildren += 2;
              } else {
                if (!_index.match.call(void 0, _types.TokenType.braceR)) {
                  numExplicitChildren++;
                  _expression.parseExpression.call(void 0);
                }
                nextJSXExprToken();
              }
              break;
            // istanbul ignore next - should never happen
            default:
              _util.unexpected.call(void 0);
              return;
          }
        }
      }
    }
    function jsxParseElement() {
      nextJSXTagToken();
      jsxParseElementAt();
    }
    exports.jsxParseElement = jsxParseElement;
    function nextJSXTagToken() {
      _base.state.tokens.push(new (0, _index.Token)());
      _index.skipSpace.call(void 0);
      _base.state.start = _base.state.pos;
      const code = _base.input.charCodeAt(_base.state.pos);
      if (_identifier.IS_IDENTIFIER_START[code]) {
        jsxReadWord();
      } else if (code === _charcodes.charCodes.quotationMark || code === _charcodes.charCodes.apostrophe) {
        jsxReadString(code);
      } else {
        ++_base.state.pos;
        switch (code) {
          case _charcodes.charCodes.greaterThan:
            _index.finishToken.call(void 0, _types.TokenType.jsxTagEnd);
            break;
          case _charcodes.charCodes.lessThan:
            _index.finishToken.call(void 0, _types.TokenType.jsxTagStart);
            break;
          case _charcodes.charCodes.slash:
            _index.finishToken.call(void 0, _types.TokenType.slash);
            break;
          case _charcodes.charCodes.equalsTo:
            _index.finishToken.call(void 0, _types.TokenType.eq);
            break;
          case _charcodes.charCodes.leftCurlyBrace:
            _index.finishToken.call(void 0, _types.TokenType.braceL);
            break;
          case _charcodes.charCodes.dot:
            _index.finishToken.call(void 0, _types.TokenType.dot);
            break;
          case _charcodes.charCodes.colon:
            _index.finishToken.call(void 0, _types.TokenType.colon);
            break;
          default:
            _util.unexpected.call(void 0);
        }
      }
    }
    exports.nextJSXTagToken = nextJSXTagToken;
    function nextJSXExprToken() {
      _base.state.tokens.push(new (0, _index.Token)());
      _base.state.start = _base.state.pos;
      jsxReadToken();
    }
  }
});

// node_modules/sucrase/dist/parser/plugins/types.js
var require_types3 = __commonJS({
  "node_modules/sucrase/dist/parser/plugins/types.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _types = require_types();
    var _base = require_base();
    var _expression = require_expression();
    var _flow = require_flow();
    var _typescript = require_typescript();
    function typedParseConditional(noIn) {
      if (_index.match.call(void 0, _types.TokenType.question)) {
        const nextType = _index.lookaheadType.call(void 0);
        if (nextType === _types.TokenType.colon || nextType === _types.TokenType.comma || nextType === _types.TokenType.parenR) {
          return;
        }
      }
      _expression.baseParseConditional.call(void 0, noIn);
    }
    exports.typedParseConditional = typedParseConditional;
    function typedParseParenItem() {
      _index.eatTypeToken.call(void 0, _types.TokenType.question);
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        if (_base.isTypeScriptEnabled) {
          _typescript.tsParseTypeAnnotation.call(void 0);
        } else if (_base.isFlowEnabled) {
          _flow.flowParseTypeAnnotation.call(void 0);
        }
      }
    }
    exports.typedParseParenItem = typedParseParenItem;
  }
});

// node_modules/sucrase/dist/parser/traverser/expression.js
var require_expression = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/expression.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _flow = require_flow();
    var _index = require_jsx();
    var _types = require_types3();
    var _typescript = require_typescript();
    var _index3 = require_tokenizer();
    var _keywords = require_keywords();
    var _state = require_state();
    var _types3 = require_types();
    var _charcodes = require_charcodes();
    var _identifier = require_identifier();
    var _base = require_base();
    var _lval = require_lval();
    var _statement = require_statement();
    var _util = require_util();
    var StopState = class {
      constructor(stop) {
        this.stop = stop;
      }
    };
    exports.StopState = StopState;
    function parseExpression(noIn = false) {
      parseMaybeAssign(noIn);
      if (_index3.match.call(void 0, _types3.TokenType.comma)) {
        while (_index3.eat.call(void 0, _types3.TokenType.comma)) {
          parseMaybeAssign(noIn);
        }
      }
    }
    exports.parseExpression = parseExpression;
    function parseMaybeAssign(noIn = false, isWithinParens = false) {
      if (_base.isTypeScriptEnabled) {
        return _typescript.tsParseMaybeAssign.call(void 0, noIn, isWithinParens);
      } else if (_base.isFlowEnabled) {
        return _flow.flowParseMaybeAssign.call(void 0, noIn, isWithinParens);
      } else {
        return baseParseMaybeAssign(noIn, isWithinParens);
      }
    }
    exports.parseMaybeAssign = parseMaybeAssign;
    function baseParseMaybeAssign(noIn, isWithinParens) {
      if (_index3.match.call(void 0, _types3.TokenType._yield)) {
        parseYield();
        return false;
      }
      if (_index3.match.call(void 0, _types3.TokenType.parenL) || _index3.match.call(void 0, _types3.TokenType.name) || _index3.match.call(void 0, _types3.TokenType._yield)) {
        _base.state.potentialArrowAt = _base.state.start;
      }
      const wasArrow = parseMaybeConditional(noIn);
      if (isWithinParens) {
        parseParenItem();
      }
      if (_base.state.type & _types3.TokenType.IS_ASSIGN) {
        _index3.next.call(void 0);
        parseMaybeAssign(noIn);
        return false;
      }
      return wasArrow;
    }
    exports.baseParseMaybeAssign = baseParseMaybeAssign;
    function parseMaybeConditional(noIn) {
      const wasArrow = parseExprOps(noIn);
      if (wasArrow) {
        return true;
      }
      parseConditional(noIn);
      return false;
    }
    function parseConditional(noIn) {
      if (_base.isTypeScriptEnabled || _base.isFlowEnabled) {
        _types.typedParseConditional.call(void 0, noIn);
      } else {
        baseParseConditional(noIn);
      }
    }
    function baseParseConditional(noIn) {
      if (_index3.eat.call(void 0, _types3.TokenType.question)) {
        parseMaybeAssign();
        _util.expect.call(void 0, _types3.TokenType.colon);
        parseMaybeAssign(noIn);
      }
    }
    exports.baseParseConditional = baseParseConditional;
    function parseExprOps(noIn) {
      const startTokenIndex = _base.state.tokens.length;
      const wasArrow = parseMaybeUnary();
      if (wasArrow) {
        return true;
      }
      parseExprOp(startTokenIndex, -1, noIn);
      return false;
    }
    function parseExprOp(startTokenIndex, minPrec, noIn) {
      if (_base.isTypeScriptEnabled && (_types3.TokenType._in & _types3.TokenType.PRECEDENCE_MASK) > minPrec && !_util.hasPrecedingLineBreak.call(void 0) && (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._as) || _util.eatContextual.call(void 0, _keywords.ContextualKeyword._satisfies))) {
        const oldIsType = _index3.pushTypeContext.call(void 0, 1);
        _typescript.tsParseType.call(void 0);
        _index3.popTypeContext.call(void 0, oldIsType);
        _index3.rescan_gt.call(void 0);
        parseExprOp(startTokenIndex, minPrec, noIn);
        return;
      }
      const prec = _base.state.type & _types3.TokenType.PRECEDENCE_MASK;
      if (prec > 0 && (!noIn || !_index3.match.call(void 0, _types3.TokenType._in))) {
        if (prec > minPrec) {
          const op = _base.state.type;
          _index3.next.call(void 0);
          if (op === _types3.TokenType.nullishCoalescing) {
            _base.state.tokens[_base.state.tokens.length - 1].nullishStartIndex = startTokenIndex;
          }
          const rhsStartTokenIndex = _base.state.tokens.length;
          parseMaybeUnary();
          parseExprOp(rhsStartTokenIndex, op & _types3.TokenType.IS_RIGHT_ASSOCIATIVE ? prec - 1 : prec, noIn);
          if (op === _types3.TokenType.nullishCoalescing) {
            _base.state.tokens[startTokenIndex].numNullishCoalesceStarts++;
            _base.state.tokens[_base.state.tokens.length - 1].numNullishCoalesceEnds++;
          }
          parseExprOp(startTokenIndex, minPrec, noIn);
        }
      }
    }
    function parseMaybeUnary() {
      if (_base.isTypeScriptEnabled && !_base.isJSXEnabled && _index3.eat.call(void 0, _types3.TokenType.lessThan)) {
        _typescript.tsParseTypeAssertion.call(void 0);
        return false;
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._module) && _index3.lookaheadCharCode.call(void 0) === _charcodes.charCodes.leftCurlyBrace && !_util.hasFollowingLineBreak.call(void 0)) {
        parseModuleExpression();
        return false;
      }
      if (_base.state.type & _types3.TokenType.IS_PREFIX) {
        _index3.next.call(void 0);
        parseMaybeUnary();
        return false;
      }
      const wasArrow = parseExprSubscripts();
      if (wasArrow) {
        return true;
      }
      while (_base.state.type & _types3.TokenType.IS_POSTFIX && !_util.canInsertSemicolon.call(void 0)) {
        if (_base.state.type === _types3.TokenType.preIncDec) {
          _base.state.type = _types3.TokenType.postIncDec;
        }
        _index3.next.call(void 0);
      }
      return false;
    }
    exports.parseMaybeUnary = parseMaybeUnary;
    function parseExprSubscripts() {
      const startTokenIndex = _base.state.tokens.length;
      const wasArrow = parseExprAtom();
      if (wasArrow) {
        return true;
      }
      parseSubscripts(startTokenIndex);
      if (_base.state.tokens.length > startTokenIndex && _base.state.tokens[startTokenIndex].isOptionalChainStart) {
        _base.state.tokens[_base.state.tokens.length - 1].isOptionalChainEnd = true;
      }
      return false;
    }
    exports.parseExprSubscripts = parseExprSubscripts;
    function parseSubscripts(startTokenIndex, noCalls = false) {
      if (_base.isFlowEnabled) {
        _flow.flowParseSubscripts.call(void 0, startTokenIndex, noCalls);
      } else {
        baseParseSubscripts(startTokenIndex, noCalls);
      }
    }
    function baseParseSubscripts(startTokenIndex, noCalls = false) {
      const stopState = new StopState(false);
      do {
        parseSubscript(startTokenIndex, noCalls, stopState);
      } while (!stopState.stop && !_base.state.error);
    }
    exports.baseParseSubscripts = baseParseSubscripts;
    function parseSubscript(startTokenIndex, noCalls, stopState) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseSubscript.call(void 0, startTokenIndex, noCalls, stopState);
      } else if (_base.isFlowEnabled) {
        _flow.flowParseSubscript.call(void 0, startTokenIndex, noCalls, stopState);
      } else {
        baseParseSubscript(startTokenIndex, noCalls, stopState);
      }
    }
    function baseParseSubscript(startTokenIndex, noCalls, stopState) {
      if (!noCalls && _index3.eat.call(void 0, _types3.TokenType.doubleColon)) {
        parseNoCallExpr();
        stopState.stop = true;
        parseSubscripts(startTokenIndex, noCalls);
      } else if (_index3.match.call(void 0, _types3.TokenType.questionDot)) {
        _base.state.tokens[startTokenIndex].isOptionalChainStart = true;
        if (noCalls && _index3.lookaheadType.call(void 0) === _types3.TokenType.parenL) {
          stopState.stop = true;
          return;
        }
        _index3.next.call(void 0);
        _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
        if (_index3.eat.call(void 0, _types3.TokenType.bracketL)) {
          parseExpression();
          _util.expect.call(void 0, _types3.TokenType.bracketR);
        } else if (_index3.eat.call(void 0, _types3.TokenType.parenL)) {
          parseCallExpressionArguments();
        } else {
          parseMaybePrivateName();
        }
      } else if (_index3.eat.call(void 0, _types3.TokenType.dot)) {
        _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
        parseMaybePrivateName();
      } else if (_index3.eat.call(void 0, _types3.TokenType.bracketL)) {
        _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
        parseExpression();
        _util.expect.call(void 0, _types3.TokenType.bracketR);
      } else if (!noCalls && _index3.match.call(void 0, _types3.TokenType.parenL)) {
        if (atPossibleAsync()) {
          const snapshot = _base.state.snapshot();
          const asyncStartTokenIndex = _base.state.tokens.length;
          _index3.next.call(void 0);
          _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
          const callContextId = _base.getNextContextId.call(void 0);
          _base.state.tokens[_base.state.tokens.length - 1].contextId = callContextId;
          parseCallExpressionArguments();
          _base.state.tokens[_base.state.tokens.length - 1].contextId = callContextId;
          if (shouldParseAsyncArrow()) {
            _base.state.restoreFromSnapshot(snapshot);
            stopState.stop = true;
            _base.state.scopeDepth++;
            _statement.parseFunctionParams.call(void 0);
            parseAsyncArrowFromCallExpression(asyncStartTokenIndex);
          }
        } else {
          _index3.next.call(void 0);
          _base.state.tokens[_base.state.tokens.length - 1].subscriptStartIndex = startTokenIndex;
          const callContextId = _base.getNextContextId.call(void 0);
          _base.state.tokens[_base.state.tokens.length - 1].contextId = callContextId;
          parseCallExpressionArguments();
          _base.state.tokens[_base.state.tokens.length - 1].contextId = callContextId;
        }
      } else if (_index3.match.call(void 0, _types3.TokenType.backQuote)) {
        parseTemplate();
      } else {
        stopState.stop = true;
      }
    }
    exports.baseParseSubscript = baseParseSubscript;
    function atPossibleAsync() {
      return _base.state.tokens[_base.state.tokens.length - 1].contextualKeyword === _keywords.ContextualKeyword._async && !_util.canInsertSemicolon.call(void 0);
    }
    exports.atPossibleAsync = atPossibleAsync;
    function parseCallExpressionArguments() {
      let first = true;
      while (!_index3.eat.call(void 0, _types3.TokenType.parenR) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types3.TokenType.comma);
          if (_index3.eat.call(void 0, _types3.TokenType.parenR)) {
            break;
          }
        }
        parseExprListItem(false);
      }
    }
    exports.parseCallExpressionArguments = parseCallExpressionArguments;
    function shouldParseAsyncArrow() {
      return _index3.match.call(void 0, _types3.TokenType.colon) || _index3.match.call(void 0, _types3.TokenType.arrow);
    }
    function parseAsyncArrowFromCallExpression(startTokenIndex) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsStartParseAsyncArrowFromCallExpression.call(void 0);
      } else if (_base.isFlowEnabled) {
        _flow.flowStartParseAsyncArrowFromCallExpression.call(void 0);
      }
      _util.expect.call(void 0, _types3.TokenType.arrow);
      parseArrowExpression(startTokenIndex);
    }
    function parseNoCallExpr() {
      const startTokenIndex = _base.state.tokens.length;
      parseExprAtom();
      parseSubscripts(startTokenIndex, true);
    }
    function parseExprAtom() {
      if (_index3.eat.call(void 0, _types3.TokenType.modulo)) {
        parseIdentifier();
        return false;
      }
      if (_index3.match.call(void 0, _types3.TokenType.jsxText) || _index3.match.call(void 0, _types3.TokenType.jsxEmptyText)) {
        parseLiteral();
        return false;
      } else if (_index3.match.call(void 0, _types3.TokenType.lessThan) && _base.isJSXEnabled) {
        _base.state.type = _types3.TokenType.jsxTagStart;
        _index.jsxParseElement.call(void 0);
        _index3.next.call(void 0);
        return false;
      }
      const canBeArrow = _base.state.potentialArrowAt === _base.state.start;
      switch (_base.state.type) {
        case _types3.TokenType.slash:
        case _types3.TokenType.assign:
          _index3.retokenizeSlashAsRegex.call(void 0);
        // Fall through.
        case _types3.TokenType._super:
        case _types3.TokenType._this:
        case _types3.TokenType.regexp:
        case _types3.TokenType.num:
        case _types3.TokenType.bigint:
        case _types3.TokenType.decimal:
        case _types3.TokenType.string:
        case _types3.TokenType._null:
        case _types3.TokenType._true:
        case _types3.TokenType._false:
          _index3.next.call(void 0);
          return false;
        case _types3.TokenType._import:
          _index3.next.call(void 0);
          if (_index3.match.call(void 0, _types3.TokenType.dot)) {
            _base.state.tokens[_base.state.tokens.length - 1].type = _types3.TokenType.name;
            _index3.next.call(void 0);
            parseIdentifier();
          }
          return false;
        case _types3.TokenType.name: {
          const startTokenIndex = _base.state.tokens.length;
          const functionStart = _base.state.start;
          const contextualKeyword = _base.state.contextualKeyword;
          parseIdentifier();
          if (contextualKeyword === _keywords.ContextualKeyword._await) {
            parseAwait();
            return false;
          } else if (contextualKeyword === _keywords.ContextualKeyword._async && _index3.match.call(void 0, _types3.TokenType._function) && !_util.canInsertSemicolon.call(void 0)) {
            _index3.next.call(void 0);
            _statement.parseFunction.call(void 0, functionStart, false);
            return false;
          } else if (canBeArrow && contextualKeyword === _keywords.ContextualKeyword._async && !_util.canInsertSemicolon.call(void 0) && _index3.match.call(void 0, _types3.TokenType.name)) {
            _base.state.scopeDepth++;
            _lval.parseBindingIdentifier.call(void 0, false);
            _util.expect.call(void 0, _types3.TokenType.arrow);
            parseArrowExpression(startTokenIndex);
            return true;
          } else if (_index3.match.call(void 0, _types3.TokenType._do) && !_util.canInsertSemicolon.call(void 0)) {
            _index3.next.call(void 0);
            _statement.parseBlock.call(void 0);
            return false;
          }
          if (canBeArrow && !_util.canInsertSemicolon.call(void 0) && _index3.match.call(void 0, _types3.TokenType.arrow)) {
            _base.state.scopeDepth++;
            _lval.markPriorBindingIdentifier.call(void 0, false);
            _util.expect.call(void 0, _types3.TokenType.arrow);
            parseArrowExpression(startTokenIndex);
            return true;
          }
          _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index3.IdentifierRole.Access;
          return false;
        }
        case _types3.TokenType._do: {
          _index3.next.call(void 0);
          _statement.parseBlock.call(void 0);
          return false;
        }
        case _types3.TokenType.parenL: {
          const wasArrow = parseParenAndDistinguishExpression(canBeArrow);
          return wasArrow;
        }
        case _types3.TokenType.bracketL:
          _index3.next.call(void 0);
          parseExprList(_types3.TokenType.bracketR, true);
          return false;
        case _types3.TokenType.braceL:
          parseObj(false, false);
          return false;
        case _types3.TokenType._function:
          parseFunctionExpression();
          return false;
        case _types3.TokenType.at:
          _statement.parseDecorators.call(void 0);
        // Fall through.
        case _types3.TokenType._class:
          _statement.parseClass.call(void 0, false);
          return false;
        case _types3.TokenType._new:
          parseNew();
          return false;
        case _types3.TokenType.backQuote:
          parseTemplate();
          return false;
        case _types3.TokenType.doubleColon: {
          _index3.next.call(void 0);
          parseNoCallExpr();
          return false;
        }
        case _types3.TokenType.hash: {
          const code = _index3.lookaheadCharCode.call(void 0);
          if (_identifier.IS_IDENTIFIER_START[code] || code === _charcodes.charCodes.backslash) {
            parseMaybePrivateName();
          } else {
            _index3.next.call(void 0);
          }
          return false;
        }
        default:
          _util.unexpected.call(void 0);
          return false;
      }
    }
    exports.parseExprAtom = parseExprAtom;
    function parseMaybePrivateName() {
      _index3.eat.call(void 0, _types3.TokenType.hash);
      parseIdentifier();
    }
    function parseFunctionExpression() {
      const functionStart = _base.state.start;
      parseIdentifier();
      if (_index3.eat.call(void 0, _types3.TokenType.dot)) {
        parseIdentifier();
      }
      _statement.parseFunction.call(void 0, functionStart, false);
    }
    function parseLiteral() {
      _index3.next.call(void 0);
    }
    exports.parseLiteral = parseLiteral;
    function parseParenExpression() {
      _util.expect.call(void 0, _types3.TokenType.parenL);
      parseExpression();
      _util.expect.call(void 0, _types3.TokenType.parenR);
    }
    exports.parseParenExpression = parseParenExpression;
    function parseParenAndDistinguishExpression(canBeArrow) {
      const snapshot = _base.state.snapshot();
      const startTokenIndex = _base.state.tokens.length;
      _util.expect.call(void 0, _types3.TokenType.parenL);
      let first = true;
      while (!_index3.match.call(void 0, _types3.TokenType.parenR) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types3.TokenType.comma);
          if (_index3.match.call(void 0, _types3.TokenType.parenR)) {
            break;
          }
        }
        if (_index3.match.call(void 0, _types3.TokenType.ellipsis)) {
          _lval.parseRest.call(
            void 0,
            false
            /* isBlockScope */
          );
          parseParenItem();
          break;
        } else {
          parseMaybeAssign(false, true);
        }
      }
      _util.expect.call(void 0, _types3.TokenType.parenR);
      if (canBeArrow && shouldParseArrow()) {
        const wasArrow = parseArrow();
        if (wasArrow) {
          _base.state.restoreFromSnapshot(snapshot);
          _base.state.scopeDepth++;
          _statement.parseFunctionParams.call(void 0);
          parseArrow();
          parseArrowExpression(startTokenIndex);
          if (_base.state.error) {
            _base.state.restoreFromSnapshot(snapshot);
            parseParenAndDistinguishExpression(false);
            return false;
          }
          return true;
        }
      }
      return false;
    }
    function shouldParseArrow() {
      return _index3.match.call(void 0, _types3.TokenType.colon) || !_util.canInsertSemicolon.call(void 0);
    }
    function parseArrow() {
      if (_base.isTypeScriptEnabled) {
        return _typescript.tsParseArrow.call(void 0);
      } else if (_base.isFlowEnabled) {
        return _flow.flowParseArrow.call(void 0);
      } else {
        return _index3.eat.call(void 0, _types3.TokenType.arrow);
      }
    }
    exports.parseArrow = parseArrow;
    function parseParenItem() {
      if (_base.isTypeScriptEnabled || _base.isFlowEnabled) {
        _types.typedParseParenItem.call(void 0);
      }
    }
    function parseNew() {
      _util.expect.call(void 0, _types3.TokenType._new);
      if (_index3.eat.call(void 0, _types3.TokenType.dot)) {
        parseIdentifier();
        return;
      }
      parseNewCallee();
      if (_base.isFlowEnabled) {
        _flow.flowStartParseNewArguments.call(void 0);
      }
      if (_index3.eat.call(void 0, _types3.TokenType.parenL)) {
        parseExprList(_types3.TokenType.parenR);
      }
    }
    function parseNewCallee() {
      parseNoCallExpr();
      _index3.eat.call(void 0, _types3.TokenType.questionDot);
    }
    function parseTemplate() {
      _index3.nextTemplateToken.call(void 0);
      _index3.nextTemplateToken.call(void 0);
      while (!_index3.match.call(void 0, _types3.TokenType.backQuote) && !_base.state.error) {
        _util.expect.call(void 0, _types3.TokenType.dollarBraceL);
        parseExpression();
        _index3.nextTemplateToken.call(void 0);
        _index3.nextTemplateToken.call(void 0);
      }
      _index3.next.call(void 0);
    }
    exports.parseTemplate = parseTemplate;
    function parseObj(isPattern, isBlockScope) {
      const contextId = _base.getNextContextId.call(void 0);
      let first = true;
      _index3.next.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
      while (!_index3.eat.call(void 0, _types3.TokenType.braceR) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types3.TokenType.comma);
          if (_index3.eat.call(void 0, _types3.TokenType.braceR)) {
            break;
          }
        }
        let isGenerator = false;
        if (_index3.match.call(void 0, _types3.TokenType.ellipsis)) {
          const previousIndex = _base.state.tokens.length;
          _lval.parseSpread.call(void 0);
          if (isPattern) {
            if (_base.state.tokens.length === previousIndex + 2) {
              _lval.markPriorBindingIdentifier.call(void 0, isBlockScope);
            }
            if (_index3.eat.call(void 0, _types3.TokenType.braceR)) {
              break;
            }
          }
          continue;
        }
        if (!isPattern) {
          isGenerator = _index3.eat.call(void 0, _types3.TokenType.star);
        }
        if (!isPattern && _util.isContextual.call(void 0, _keywords.ContextualKeyword._async)) {
          if (isGenerator) _util.unexpected.call(void 0);
          parseIdentifier();
          if (_index3.match.call(void 0, _types3.TokenType.colon) || _index3.match.call(void 0, _types3.TokenType.parenL) || _index3.match.call(void 0, _types3.TokenType.braceR) || _index3.match.call(void 0, _types3.TokenType.eq) || _index3.match.call(void 0, _types3.TokenType.comma)) {
          } else {
            if (_index3.match.call(void 0, _types3.TokenType.star)) {
              _index3.next.call(void 0);
              isGenerator = true;
            }
            parsePropertyName(contextId);
          }
        } else {
          parsePropertyName(contextId);
        }
        parseObjPropValue(isPattern, isBlockScope, contextId);
      }
      _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
    }
    exports.parseObj = parseObj;
    function isGetterOrSetterMethod(isPattern) {
      return !isPattern && (_index3.match.call(void 0, _types3.TokenType.string) || // get "string"() {}
      _index3.match.call(void 0, _types3.TokenType.num) || // get 1() {}
      _index3.match.call(void 0, _types3.TokenType.bracketL) || // get ["string"]() {}
      _index3.match.call(void 0, _types3.TokenType.name) || // get foo() {}
      !!(_base.state.type & _types3.TokenType.IS_KEYWORD));
    }
    function parseObjectMethod(isPattern, objectContextId) {
      const functionStart = _base.state.start;
      if (_index3.match.call(void 0, _types3.TokenType.parenL)) {
        if (isPattern) _util.unexpected.call(void 0);
        parseMethod(
          functionStart,
          /* isConstructor */
          false
        );
        return true;
      }
      if (isGetterOrSetterMethod(isPattern)) {
        parsePropertyName(objectContextId);
        parseMethod(
          functionStart,
          /* isConstructor */
          false
        );
        return true;
      }
      return false;
    }
    function parseObjectProperty(isPattern, isBlockScope) {
      if (_index3.eat.call(void 0, _types3.TokenType.colon)) {
        if (isPattern) {
          _lval.parseMaybeDefault.call(void 0, isBlockScope);
        } else {
          parseMaybeAssign(false);
        }
        return;
      }
      let identifierRole;
      if (isPattern) {
        if (_base.state.scopeDepth === 0) {
          identifierRole = _index3.IdentifierRole.ObjectShorthandTopLevelDeclaration;
        } else if (isBlockScope) {
          identifierRole = _index3.IdentifierRole.ObjectShorthandBlockScopedDeclaration;
        } else {
          identifierRole = _index3.IdentifierRole.ObjectShorthandFunctionScopedDeclaration;
        }
      } else {
        identifierRole = _index3.IdentifierRole.ObjectShorthand;
      }
      _base.state.tokens[_base.state.tokens.length - 1].identifierRole = identifierRole;
      _lval.parseMaybeDefault.call(void 0, isBlockScope, true);
    }
    function parseObjPropValue(isPattern, isBlockScope, objectContextId) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsStartParseObjPropValue.call(void 0);
      } else if (_base.isFlowEnabled) {
        _flow.flowStartParseObjPropValue.call(void 0);
      }
      const wasMethod = parseObjectMethod(isPattern, objectContextId);
      if (!wasMethod) {
        parseObjectProperty(isPattern, isBlockScope);
      }
    }
    function parsePropertyName(objectContextId) {
      if (_base.isFlowEnabled) {
        _flow.flowParseVariance.call(void 0);
      }
      if (_index3.eat.call(void 0, _types3.TokenType.bracketL)) {
        _base.state.tokens[_base.state.tokens.length - 1].contextId = objectContextId;
        parseMaybeAssign();
        _util.expect.call(void 0, _types3.TokenType.bracketR);
        _base.state.tokens[_base.state.tokens.length - 1].contextId = objectContextId;
      } else {
        if (_index3.match.call(void 0, _types3.TokenType.num) || _index3.match.call(void 0, _types3.TokenType.string) || _index3.match.call(void 0, _types3.TokenType.bigint) || _index3.match.call(void 0, _types3.TokenType.decimal)) {
          parseExprAtom();
        } else {
          parseMaybePrivateName();
        }
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _index3.IdentifierRole.ObjectKey;
        _base.state.tokens[_base.state.tokens.length - 1].contextId = objectContextId;
      }
    }
    exports.parsePropertyName = parsePropertyName;
    function parseMethod(functionStart, isConstructor) {
      const funcContextId = _base.getNextContextId.call(void 0);
      _base.state.scopeDepth++;
      const startTokenIndex = _base.state.tokens.length;
      const allowModifiers = isConstructor;
      _statement.parseFunctionParams.call(void 0, allowModifiers, funcContextId);
      parseFunctionBodyAndFinish(functionStart, funcContextId);
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, true));
      _base.state.scopeDepth--;
    }
    exports.parseMethod = parseMethod;
    function parseArrowExpression(startTokenIndex) {
      parseFunctionBody(true);
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, true));
      _base.state.scopeDepth--;
    }
    exports.parseArrowExpression = parseArrowExpression;
    function parseFunctionBodyAndFinish(functionStart, funcContextId = 0) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseFunctionBodyAndFinish.call(void 0, functionStart, funcContextId);
      } else if (_base.isFlowEnabled) {
        _flow.flowParseFunctionBodyAndFinish.call(void 0, funcContextId);
      } else {
        parseFunctionBody(false, funcContextId);
      }
    }
    exports.parseFunctionBodyAndFinish = parseFunctionBodyAndFinish;
    function parseFunctionBody(allowExpression, funcContextId = 0) {
      const isExpression = allowExpression && !_index3.match.call(void 0, _types3.TokenType.braceL);
      if (isExpression) {
        parseMaybeAssign();
      } else {
        _statement.parseBlock.call(void 0, true, funcContextId);
      }
    }
    exports.parseFunctionBody = parseFunctionBody;
    function parseExprList(close, allowEmpty = false) {
      let first = true;
      while (!_index3.eat.call(void 0, close) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types3.TokenType.comma);
          if (_index3.eat.call(void 0, close)) break;
        }
        parseExprListItem(allowEmpty);
      }
    }
    function parseExprListItem(allowEmpty) {
      if (allowEmpty && _index3.match.call(void 0, _types3.TokenType.comma)) {
      } else if (_index3.match.call(void 0, _types3.TokenType.ellipsis)) {
        _lval.parseSpread.call(void 0);
        parseParenItem();
      } else if (_index3.match.call(void 0, _types3.TokenType.question)) {
        _index3.next.call(void 0);
      } else {
        parseMaybeAssign(false, true);
      }
    }
    function parseIdentifier() {
      _index3.next.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].type = _types3.TokenType.name;
    }
    exports.parseIdentifier = parseIdentifier;
    function parseAwait() {
      parseMaybeUnary();
    }
    function parseYield() {
      _index3.next.call(void 0);
      if (!_index3.match.call(void 0, _types3.TokenType.semi) && !_util.canInsertSemicolon.call(void 0)) {
        _index3.eat.call(void 0, _types3.TokenType.star);
        parseMaybeAssign();
      }
    }
    function parseModuleExpression() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._module);
      _util.expect.call(void 0, _types3.TokenType.braceL);
      _statement.parseBlockBody.call(void 0, _types3.TokenType.braceR);
    }
  }
});

// node_modules/sucrase/dist/parser/plugins/flow.js
var require_flow = __commonJS({
  "node_modules/sucrase/dist/parser/plugins/flow.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _keywords = require_keywords();
    var _types = require_types();
    var _base = require_base();
    var _expression = require_expression();
    var _statement = require_statement();
    var _util = require_util();
    function isMaybeDefaultImport(lookahead2) {
      return (lookahead2.type === _types.TokenType.name || !!(lookahead2.type & _types.TokenType.IS_KEYWORD)) && lookahead2.contextualKeyword !== _keywords.ContextualKeyword._from;
    }
    function flowParseTypeInitialiser(tok) {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, tok || _types.TokenType.colon);
      flowParseType();
      _index.popTypeContext.call(void 0, oldIsType);
    }
    function flowParsePredicate() {
      _util.expect.call(void 0, _types.TokenType.modulo);
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._checks);
      if (_index.eat.call(void 0, _types.TokenType.parenL)) {
        _expression.parseExpression.call(void 0);
        _util.expect.call(void 0, _types.TokenType.parenR);
      }
    }
    function flowParseTypeAndPredicateInitialiser() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, _types.TokenType.colon);
      if (_index.match.call(void 0, _types.TokenType.modulo)) {
        flowParsePredicate();
      } else {
        flowParseType();
        if (_index.match.call(void 0, _types.TokenType.modulo)) {
          flowParsePredicate();
        }
      }
      _index.popTypeContext.call(void 0, oldIsType);
    }
    function flowParseDeclareClass() {
      _index.next.call(void 0);
      flowParseInterfaceish(
        /* isClass */
        true
      );
    }
    function flowParseDeclareFunction() {
      _index.next.call(void 0);
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
      }
      _util.expect.call(void 0, _types.TokenType.parenL);
      flowParseFunctionTypeParams();
      _util.expect.call(void 0, _types.TokenType.parenR);
      flowParseTypeAndPredicateInitialiser();
      _util.semicolon.call(void 0);
    }
    function flowParseDeclare() {
      if (_index.match.call(void 0, _types.TokenType._class)) {
        flowParseDeclareClass();
      } else if (_index.match.call(void 0, _types.TokenType._function)) {
        flowParseDeclareFunction();
      } else if (_index.match.call(void 0, _types.TokenType._var)) {
        flowParseDeclareVariable();
      } else if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._module)) {
        if (_index.eat.call(void 0, _types.TokenType.dot)) {
          flowParseDeclareModuleExports();
        } else {
          flowParseDeclareModule();
        }
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._type)) {
        flowParseDeclareTypeAlias();
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._opaque)) {
        flowParseDeclareOpaqueType();
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._interface)) {
        flowParseDeclareInterface();
      } else if (_index.match.call(void 0, _types.TokenType._export)) {
        flowParseDeclareExportDeclaration();
      } else {
        _util.unexpected.call(void 0);
      }
    }
    function flowParseDeclareVariable() {
      _index.next.call(void 0);
      flowParseTypeAnnotatableIdentifier();
      _util.semicolon.call(void 0);
    }
    function flowParseDeclareModule() {
      if (_index.match.call(void 0, _types.TokenType.string)) {
        _expression.parseExprAtom.call(void 0);
      } else {
        _expression.parseIdentifier.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_index.match.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (_index.match.call(void 0, _types.TokenType._import)) {
          _index.next.call(void 0);
          _statement.parseImport.call(void 0);
        } else {
          _util.unexpected.call(void 0);
        }
      }
      _util.expect.call(void 0, _types.TokenType.braceR);
    }
    function flowParseDeclareExportDeclaration() {
      _util.expect.call(void 0, _types.TokenType._export);
      if (_index.eat.call(void 0, _types.TokenType._default)) {
        if (_index.match.call(void 0, _types.TokenType._function) || _index.match.call(void 0, _types.TokenType._class)) {
          flowParseDeclare();
        } else {
          flowParseType();
          _util.semicolon.call(void 0);
        }
      } else if (_index.match.call(void 0, _types.TokenType._var) || // declare export var ...
      _index.match.call(void 0, _types.TokenType._function) || // declare export function ...
      _index.match.call(void 0, _types.TokenType._class) || // declare export class ...
      _util.isContextual.call(void 0, _keywords.ContextualKeyword._opaque)) {
        flowParseDeclare();
      } else if (_index.match.call(void 0, _types.TokenType.star) || // declare export * from ''
      _index.match.call(void 0, _types.TokenType.braceL) || // declare export {} ...
      _util.isContextual.call(void 0, _keywords.ContextualKeyword._interface) || // declare export interface ...
      _util.isContextual.call(void 0, _keywords.ContextualKeyword._type) || // declare export type ...
      _util.isContextual.call(void 0, _keywords.ContextualKeyword._opaque)) {
        _statement.parseExport.call(void 0);
      } else {
        _util.unexpected.call(void 0);
      }
    }
    function flowParseDeclareModuleExports() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._exports);
      flowParseTypeAnnotation();
      _util.semicolon.call(void 0);
    }
    function flowParseDeclareTypeAlias() {
      _index.next.call(void 0);
      flowParseTypeAlias();
    }
    function flowParseDeclareOpaqueType() {
      _index.next.call(void 0);
      flowParseOpaqueType(true);
    }
    function flowParseDeclareInterface() {
      _index.next.call(void 0);
      flowParseInterfaceish();
    }
    function flowParseInterfaceish(isClass = false) {
      flowParseRestrictedIdentifier();
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
      }
      if (_index.eat.call(void 0, _types.TokenType._extends)) {
        do {
          flowParseInterfaceExtends();
        } while (!isClass && _index.eat.call(void 0, _types.TokenType.comma));
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._mixins)) {
        _index.next.call(void 0);
        do {
          flowParseInterfaceExtends();
        } while (_index.eat.call(void 0, _types.TokenType.comma));
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._implements)) {
        _index.next.call(void 0);
        do {
          flowParseInterfaceExtends();
        } while (_index.eat.call(void 0, _types.TokenType.comma));
      }
      flowParseObjectType(isClass, false, isClass);
    }
    function flowParseInterfaceExtends() {
      flowParseQualifiedTypeIdentifier(false);
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterInstantiation();
      }
    }
    function flowParseInterface() {
      flowParseInterfaceish();
    }
    function flowParseRestrictedIdentifier() {
      _expression.parseIdentifier.call(void 0);
    }
    function flowParseTypeAlias() {
      flowParseRestrictedIdentifier();
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
      }
      flowParseTypeInitialiser(_types.TokenType.eq);
      _util.semicolon.call(void 0);
    }
    function flowParseOpaqueType(declare) {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._type);
      flowParseRestrictedIdentifier();
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
      }
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        flowParseTypeInitialiser(_types.TokenType.colon);
      }
      if (!declare) {
        flowParseTypeInitialiser(_types.TokenType.eq);
      }
      _util.semicolon.call(void 0);
    }
    function flowParseTypeParameter() {
      flowParseVariance();
      flowParseTypeAnnotatableIdentifier();
      if (_index.eat.call(void 0, _types.TokenType.eq)) {
        flowParseType();
      }
    }
    function flowParseTypeParameterDeclaration() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.typeParameterStart)) {
        _index.next.call(void 0);
      } else {
        _util.unexpected.call(void 0);
      }
      do {
        flowParseTypeParameter();
        if (!_index.match.call(void 0, _types.TokenType.greaterThan)) {
          _util.expect.call(void 0, _types.TokenType.comma);
        }
      } while (!_index.match.call(void 0, _types.TokenType.greaterThan) && !_base.state.error);
      _util.expect.call(void 0, _types.TokenType.greaterThan);
      _index.popTypeContext.call(void 0, oldIsType);
    }
    exports.flowParseTypeParameterDeclaration = flowParseTypeParameterDeclaration;
    function flowParseTypeParameterInstantiation() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _util.expect.call(void 0, _types.TokenType.lessThan);
      while (!_index.match.call(void 0, _types.TokenType.greaterThan) && !_base.state.error) {
        flowParseType();
        if (!_index.match.call(void 0, _types.TokenType.greaterThan)) {
          _util.expect.call(void 0, _types.TokenType.comma);
        }
      }
      _util.expect.call(void 0, _types.TokenType.greaterThan);
      _index.popTypeContext.call(void 0, oldIsType);
    }
    function flowParseInterfaceType() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._interface);
      if (_index.eat.call(void 0, _types.TokenType._extends)) {
        do {
          flowParseInterfaceExtends();
        } while (_index.eat.call(void 0, _types.TokenType.comma));
      }
      flowParseObjectType(false, false, false);
    }
    function flowParseObjectPropertyKey() {
      if (_index.match.call(void 0, _types.TokenType.num) || _index.match.call(void 0, _types.TokenType.string)) {
        _expression.parseExprAtom.call(void 0);
      } else {
        _expression.parseIdentifier.call(void 0);
      }
    }
    function flowParseObjectTypeIndexer() {
      if (_index.lookaheadType.call(void 0) === _types.TokenType.colon) {
        flowParseObjectPropertyKey();
        flowParseTypeInitialiser();
      } else {
        flowParseType();
      }
      _util.expect.call(void 0, _types.TokenType.bracketR);
      flowParseTypeInitialiser();
    }
    function flowParseObjectTypeInternalSlot() {
      flowParseObjectPropertyKey();
      _util.expect.call(void 0, _types.TokenType.bracketR);
      _util.expect.call(void 0, _types.TokenType.bracketR);
      if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.parenL)) {
        flowParseObjectTypeMethodish();
      } else {
        _index.eat.call(void 0, _types.TokenType.question);
        flowParseTypeInitialiser();
      }
    }
    function flowParseObjectTypeMethodish() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
      }
      _util.expect.call(void 0, _types.TokenType.parenL);
      while (!_index.match.call(void 0, _types.TokenType.parenR) && !_index.match.call(void 0, _types.TokenType.ellipsis) && !_base.state.error) {
        flowParseFunctionTypeParam();
        if (!_index.match.call(void 0, _types.TokenType.parenR)) {
          _util.expect.call(void 0, _types.TokenType.comma);
        }
      }
      if (_index.eat.call(void 0, _types.TokenType.ellipsis)) {
        flowParseFunctionTypeParam();
      }
      _util.expect.call(void 0, _types.TokenType.parenR);
      flowParseTypeInitialiser();
    }
    function flowParseObjectTypeCallProperty() {
      flowParseObjectTypeMethodish();
    }
    function flowParseObjectType(allowStatic, allowExact, allowProto) {
      let endDelim;
      if (allowExact && _index.match.call(void 0, _types.TokenType.braceBarL)) {
        _util.expect.call(void 0, _types.TokenType.braceBarL);
        endDelim = _types.TokenType.braceBarR;
      } else {
        _util.expect.call(void 0, _types.TokenType.braceL);
        endDelim = _types.TokenType.braceR;
      }
      while (!_index.match.call(void 0, endDelim) && !_base.state.error) {
        if (allowProto && _util.isContextual.call(void 0, _keywords.ContextualKeyword._proto)) {
          const lookahead2 = _index.lookaheadType.call(void 0);
          if (lookahead2 !== _types.TokenType.colon && lookahead2 !== _types.TokenType.question) {
            _index.next.call(void 0);
            allowStatic = false;
          }
        }
        if (allowStatic && _util.isContextual.call(void 0, _keywords.ContextualKeyword._static)) {
          const lookahead2 = _index.lookaheadType.call(void 0);
          if (lookahead2 !== _types.TokenType.colon && lookahead2 !== _types.TokenType.question) {
            _index.next.call(void 0);
          }
        }
        flowParseVariance();
        if (_index.eat.call(void 0, _types.TokenType.bracketL)) {
          if (_index.eat.call(void 0, _types.TokenType.bracketL)) {
            flowParseObjectTypeInternalSlot();
          } else {
            flowParseObjectTypeIndexer();
          }
        } else if (_index.match.call(void 0, _types.TokenType.parenL) || _index.match.call(void 0, _types.TokenType.lessThan)) {
          flowParseObjectTypeCallProperty();
        } else {
          if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._get) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._set)) {
            const lookahead2 = _index.lookaheadType.call(void 0);
            if (lookahead2 === _types.TokenType.name || lookahead2 === _types.TokenType.string || lookahead2 === _types.TokenType.num) {
              _index.next.call(void 0);
            }
          }
          flowParseObjectTypeProperty();
        }
        flowObjectTypeSemicolon();
      }
      _util.expect.call(void 0, endDelim);
    }
    function flowParseObjectTypeProperty() {
      if (_index.match.call(void 0, _types.TokenType.ellipsis)) {
        _util.expect.call(void 0, _types.TokenType.ellipsis);
        if (!_index.eat.call(void 0, _types.TokenType.comma)) {
          _index.eat.call(void 0, _types.TokenType.semi);
        }
        if (_index.match.call(void 0, _types.TokenType.braceR)) {
          return;
        }
        flowParseType();
      } else {
        flowParseObjectPropertyKey();
        if (_index.match.call(void 0, _types.TokenType.lessThan) || _index.match.call(void 0, _types.TokenType.parenL)) {
          flowParseObjectTypeMethodish();
        } else {
          _index.eat.call(void 0, _types.TokenType.question);
          flowParseTypeInitialiser();
        }
      }
    }
    function flowObjectTypeSemicolon() {
      if (!_index.eat.call(void 0, _types.TokenType.semi) && !_index.eat.call(void 0, _types.TokenType.comma) && !_index.match.call(void 0, _types.TokenType.braceR) && !_index.match.call(void 0, _types.TokenType.braceBarR)) {
        _util.unexpected.call(void 0);
      }
    }
    function flowParseQualifiedTypeIdentifier(initialIdAlreadyParsed) {
      if (!initialIdAlreadyParsed) {
        _expression.parseIdentifier.call(void 0);
      }
      while (_index.eat.call(void 0, _types.TokenType.dot)) {
        _expression.parseIdentifier.call(void 0);
      }
    }
    function flowParseGenericType() {
      flowParseQualifiedTypeIdentifier(true);
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterInstantiation();
      }
    }
    function flowParseTypeofType() {
      _util.expect.call(void 0, _types.TokenType._typeof);
      flowParsePrimaryType();
    }
    function flowParseTupleType() {
      _util.expect.call(void 0, _types.TokenType.bracketL);
      while (_base.state.pos < _base.input.length && !_index.match.call(void 0, _types.TokenType.bracketR)) {
        flowParseType();
        if (_index.match.call(void 0, _types.TokenType.bracketR)) {
          break;
        }
        _util.expect.call(void 0, _types.TokenType.comma);
      }
      _util.expect.call(void 0, _types.TokenType.bracketR);
    }
    function flowParseFunctionTypeParam() {
      const lookahead2 = _index.lookaheadType.call(void 0);
      if (lookahead2 === _types.TokenType.colon || lookahead2 === _types.TokenType.question) {
        _expression.parseIdentifier.call(void 0);
        _index.eat.call(void 0, _types.TokenType.question);
        flowParseTypeInitialiser();
      } else {
        flowParseType();
      }
    }
    function flowParseFunctionTypeParams() {
      while (!_index.match.call(void 0, _types.TokenType.parenR) && !_index.match.call(void 0, _types.TokenType.ellipsis) && !_base.state.error) {
        flowParseFunctionTypeParam();
        if (!_index.match.call(void 0, _types.TokenType.parenR)) {
          _util.expect.call(void 0, _types.TokenType.comma);
        }
      }
      if (_index.eat.call(void 0, _types.TokenType.ellipsis)) {
        flowParseFunctionTypeParam();
      }
    }
    function flowParsePrimaryType() {
      let isGroupedType = false;
      const oldNoAnonFunctionType = _base.state.noAnonFunctionType;
      switch (_base.state.type) {
        case _types.TokenType.name: {
          if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._interface)) {
            flowParseInterfaceType();
            return;
          }
          _expression.parseIdentifier.call(void 0);
          flowParseGenericType();
          return;
        }
        case _types.TokenType.braceL:
          flowParseObjectType(false, false, false);
          return;
        case _types.TokenType.braceBarL:
          flowParseObjectType(false, true, false);
          return;
        case _types.TokenType.bracketL:
          flowParseTupleType();
          return;
        case _types.TokenType.lessThan:
          flowParseTypeParameterDeclaration();
          _util.expect.call(void 0, _types.TokenType.parenL);
          flowParseFunctionTypeParams();
          _util.expect.call(void 0, _types.TokenType.parenR);
          _util.expect.call(void 0, _types.TokenType.arrow);
          flowParseType();
          return;
        case _types.TokenType.parenL:
          _index.next.call(void 0);
          if (!_index.match.call(void 0, _types.TokenType.parenR) && !_index.match.call(void 0, _types.TokenType.ellipsis)) {
            if (_index.match.call(void 0, _types.TokenType.name)) {
              const token = _index.lookaheadType.call(void 0);
              isGroupedType = token !== _types.TokenType.question && token !== _types.TokenType.colon;
            } else {
              isGroupedType = true;
            }
          }
          if (isGroupedType) {
            _base.state.noAnonFunctionType = false;
            flowParseType();
            _base.state.noAnonFunctionType = oldNoAnonFunctionType;
            if (_base.state.noAnonFunctionType || !(_index.match.call(void 0, _types.TokenType.comma) || _index.match.call(void 0, _types.TokenType.parenR) && _index.lookaheadType.call(void 0) === _types.TokenType.arrow)) {
              _util.expect.call(void 0, _types.TokenType.parenR);
              return;
            } else {
              _index.eat.call(void 0, _types.TokenType.comma);
            }
          }
          flowParseFunctionTypeParams();
          _util.expect.call(void 0, _types.TokenType.parenR);
          _util.expect.call(void 0, _types.TokenType.arrow);
          flowParseType();
          return;
        case _types.TokenType.minus:
          _index.next.call(void 0);
          _expression.parseLiteral.call(void 0);
          return;
        case _types.TokenType.string:
        case _types.TokenType.num:
        case _types.TokenType._true:
        case _types.TokenType._false:
        case _types.TokenType._null:
        case _types.TokenType._this:
        case _types.TokenType._void:
        case _types.TokenType.star:
          _index.next.call(void 0);
          return;
        default:
          if (_base.state.type === _types.TokenType._typeof) {
            flowParseTypeofType();
            return;
          } else if (_base.state.type & _types.TokenType.IS_KEYWORD) {
            _index.next.call(void 0);
            _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType.name;
            return;
          }
      }
      _util.unexpected.call(void 0);
    }
    function flowParsePostfixType() {
      flowParsePrimaryType();
      while (!_util.canInsertSemicolon.call(void 0) && (_index.match.call(void 0, _types.TokenType.bracketL) || _index.match.call(void 0, _types.TokenType.questionDot))) {
        _index.eat.call(void 0, _types.TokenType.questionDot);
        _util.expect.call(void 0, _types.TokenType.bracketL);
        if (_index.eat.call(void 0, _types.TokenType.bracketR)) {
        } else {
          flowParseType();
          _util.expect.call(void 0, _types.TokenType.bracketR);
        }
      }
    }
    function flowParsePrefixType() {
      if (_index.eat.call(void 0, _types.TokenType.question)) {
        flowParsePrefixType();
      } else {
        flowParsePostfixType();
      }
    }
    function flowParseAnonFunctionWithoutParens() {
      flowParsePrefixType();
      if (!_base.state.noAnonFunctionType && _index.eat.call(void 0, _types.TokenType.arrow)) {
        flowParseType();
      }
    }
    function flowParseIntersectionType() {
      _index.eat.call(void 0, _types.TokenType.bitwiseAND);
      flowParseAnonFunctionWithoutParens();
      while (_index.eat.call(void 0, _types.TokenType.bitwiseAND)) {
        flowParseAnonFunctionWithoutParens();
      }
    }
    function flowParseUnionType() {
      _index.eat.call(void 0, _types.TokenType.bitwiseOR);
      flowParseIntersectionType();
      while (_index.eat.call(void 0, _types.TokenType.bitwiseOR)) {
        flowParseIntersectionType();
      }
    }
    function flowParseType() {
      flowParseUnionType();
    }
    function flowParseTypeAnnotation() {
      flowParseTypeInitialiser();
    }
    exports.flowParseTypeAnnotation = flowParseTypeAnnotation;
    function flowParseTypeAnnotatableIdentifier() {
      _expression.parseIdentifier.call(void 0);
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        flowParseTypeAnnotation();
      }
    }
    function flowParseVariance() {
      if (_index.match.call(void 0, _types.TokenType.plus) || _index.match.call(void 0, _types.TokenType.minus)) {
        _index.next.call(void 0);
        _base.state.tokens[_base.state.tokens.length - 1].isType = true;
      }
    }
    exports.flowParseVariance = flowParseVariance;
    function flowParseFunctionBodyAndFinish(funcContextId) {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        flowParseTypeAndPredicateInitialiser();
      }
      _expression.parseFunctionBody.call(void 0, false, funcContextId);
    }
    exports.flowParseFunctionBodyAndFinish = flowParseFunctionBodyAndFinish;
    function flowParseSubscript(startTokenIndex, noCalls, stopState) {
      if (_index.match.call(void 0, _types.TokenType.questionDot) && _index.lookaheadType.call(void 0) === _types.TokenType.lessThan) {
        if (noCalls) {
          stopState.stop = true;
          return;
        }
        _index.next.call(void 0);
        flowParseTypeParameterInstantiation();
        _util.expect.call(void 0, _types.TokenType.parenL);
        _expression.parseCallExpressionArguments.call(void 0);
        return;
      } else if (!noCalls && _index.match.call(void 0, _types.TokenType.lessThan)) {
        const snapshot = _base.state.snapshot();
        flowParseTypeParameterInstantiation();
        _util.expect.call(void 0, _types.TokenType.parenL);
        _expression.parseCallExpressionArguments.call(void 0);
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
        } else {
          return;
        }
      }
      _expression.baseParseSubscript.call(void 0, startTokenIndex, noCalls, stopState);
    }
    exports.flowParseSubscript = flowParseSubscript;
    function flowStartParseNewArguments() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        const snapshot = _base.state.snapshot();
        flowParseTypeParameterInstantiation();
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
        }
      }
    }
    exports.flowStartParseNewArguments = flowStartParseNewArguments;
    function flowTryParseStatement() {
      if (_index.match.call(void 0, _types.TokenType.name) && _base.state.contextualKeyword === _keywords.ContextualKeyword._interface) {
        const oldIsType = _index.pushTypeContext.call(void 0, 0);
        _index.next.call(void 0);
        flowParseInterface();
        _index.popTypeContext.call(void 0, oldIsType);
        return true;
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._enum)) {
        flowParseEnumDeclaration();
        return true;
      }
      return false;
    }
    exports.flowTryParseStatement = flowTryParseStatement;
    function flowTryParseExportDefaultExpression() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._enum)) {
        flowParseEnumDeclaration();
        return true;
      }
      return false;
    }
    exports.flowTryParseExportDefaultExpression = flowTryParseExportDefaultExpression;
    function flowParseIdentifierStatement(contextualKeyword) {
      if (contextualKeyword === _keywords.ContextualKeyword._declare) {
        if (_index.match.call(void 0, _types.TokenType._class) || _index.match.call(void 0, _types.TokenType.name) || _index.match.call(void 0, _types.TokenType._function) || _index.match.call(void 0, _types.TokenType._var) || _index.match.call(void 0, _types.TokenType._export)) {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          flowParseDeclare();
          _index.popTypeContext.call(void 0, oldIsType);
        }
      } else if (_index.match.call(void 0, _types.TokenType.name)) {
        if (contextualKeyword === _keywords.ContextualKeyword._interface) {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          flowParseInterface();
          _index.popTypeContext.call(void 0, oldIsType);
        } else if (contextualKeyword === _keywords.ContextualKeyword._type) {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          flowParseTypeAlias();
          _index.popTypeContext.call(void 0, oldIsType);
        } else if (contextualKeyword === _keywords.ContextualKeyword._opaque) {
          const oldIsType = _index.pushTypeContext.call(void 0, 1);
          flowParseOpaqueType(false);
          _index.popTypeContext.call(void 0, oldIsType);
        }
      }
      _util.semicolon.call(void 0);
    }
    exports.flowParseIdentifierStatement = flowParseIdentifierStatement;
    function flowShouldParseExportDeclaration() {
      return _util.isContextual.call(void 0, _keywords.ContextualKeyword._type) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._interface) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._opaque) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._enum);
    }
    exports.flowShouldParseExportDeclaration = flowShouldParseExportDeclaration;
    function flowShouldDisallowExportDefaultSpecifier() {
      return _index.match.call(void 0, _types.TokenType.name) && (_base.state.contextualKeyword === _keywords.ContextualKeyword._type || _base.state.contextualKeyword === _keywords.ContextualKeyword._interface || _base.state.contextualKeyword === _keywords.ContextualKeyword._opaque || _base.state.contextualKeyword === _keywords.ContextualKeyword._enum);
    }
    exports.flowShouldDisallowExportDefaultSpecifier = flowShouldDisallowExportDefaultSpecifier;
    function flowParseExportDeclaration() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._type)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 1);
        _index.next.call(void 0);
        if (_index.match.call(void 0, _types.TokenType.braceL)) {
          _statement.parseExportSpecifiers.call(void 0);
          _statement.parseExportFrom.call(void 0);
        } else {
          flowParseTypeAlias();
        }
        _index.popTypeContext.call(void 0, oldIsType);
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._opaque)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 1);
        _index.next.call(void 0);
        flowParseOpaqueType(false);
        _index.popTypeContext.call(void 0, oldIsType);
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._interface)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 1);
        _index.next.call(void 0);
        flowParseInterface();
        _index.popTypeContext.call(void 0, oldIsType);
      } else {
        _statement.parseStatement.call(void 0, true);
      }
    }
    exports.flowParseExportDeclaration = flowParseExportDeclaration;
    function flowShouldParseExportStar() {
      return _index.match.call(void 0, _types.TokenType.star) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._type) && _index.lookaheadType.call(void 0) === _types.TokenType.star;
    }
    exports.flowShouldParseExportStar = flowShouldParseExportStar;
    function flowParseExportStar() {
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._type)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 2);
        _statement.baseParseExportStar.call(void 0);
        _index.popTypeContext.call(void 0, oldIsType);
      } else {
        _statement.baseParseExportStar.call(void 0);
      }
    }
    exports.flowParseExportStar = flowParseExportStar;
    function flowAfterParseClassSuper(hasSuper) {
      if (hasSuper && _index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterInstantiation();
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._implements)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 0);
        _index.next.call(void 0);
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._implements;
        do {
          flowParseRestrictedIdentifier();
          if (_index.match.call(void 0, _types.TokenType.lessThan)) {
            flowParseTypeParameterInstantiation();
          }
        } while (_index.eat.call(void 0, _types.TokenType.comma));
        _index.popTypeContext.call(void 0, oldIsType);
      }
    }
    exports.flowAfterParseClassSuper = flowAfterParseClassSuper;
    function flowStartParseObjPropValue() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        flowParseTypeParameterDeclaration();
        if (!_index.match.call(void 0, _types.TokenType.parenL)) _util.unexpected.call(void 0);
      }
    }
    exports.flowStartParseObjPropValue = flowStartParseObjPropValue;
    function flowParseAssignableListItemTypes() {
      const oldIsType = _index.pushTypeContext.call(void 0, 0);
      _index.eat.call(void 0, _types.TokenType.question);
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        flowParseTypeAnnotation();
      }
      _index.popTypeContext.call(void 0, oldIsType);
    }
    exports.flowParseAssignableListItemTypes = flowParseAssignableListItemTypes;
    function flowStartParseImportSpecifiers() {
      if (_index.match.call(void 0, _types.TokenType._typeof) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._type)) {
        const lh = _index.lookaheadTypeAndKeyword.call(void 0);
        if (isMaybeDefaultImport(lh) || lh.type === _types.TokenType.braceL || lh.type === _types.TokenType.star) {
          _index.next.call(void 0);
        }
      }
    }
    exports.flowStartParseImportSpecifiers = flowStartParseImportSpecifiers;
    function flowParseImportSpecifier() {
      const isTypeKeyword = _base.state.contextualKeyword === _keywords.ContextualKeyword._type || _base.state.type === _types.TokenType._typeof;
      if (isTypeKeyword) {
        _index.next.call(void 0);
      } else {
        _expression.parseIdentifier.call(void 0);
      }
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._as) && !_util.isLookaheadContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        _expression.parseIdentifier.call(void 0);
        if (isTypeKeyword && !_index.match.call(void 0, _types.TokenType.name) && !(_base.state.type & _types.TokenType.IS_KEYWORD)) {
        } else {
          _expression.parseIdentifier.call(void 0);
        }
      } else {
        if (isTypeKeyword && (_index.match.call(void 0, _types.TokenType.name) || !!(_base.state.type & _types.TokenType.IS_KEYWORD))) {
          _expression.parseIdentifier.call(void 0);
        }
        if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._as)) {
          _expression.parseIdentifier.call(void 0);
        }
      }
    }
    exports.flowParseImportSpecifier = flowParseImportSpecifier;
    function flowStartParseFunctionParams() {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 0);
        flowParseTypeParameterDeclaration();
        _index.popTypeContext.call(void 0, oldIsType);
      }
    }
    exports.flowStartParseFunctionParams = flowStartParseFunctionParams;
    function flowAfterParseVarHead() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        flowParseTypeAnnotation();
      }
    }
    exports.flowAfterParseVarHead = flowAfterParseVarHead;
    function flowStartParseAsyncArrowFromCallExpression() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        const oldNoAnonFunctionType = _base.state.noAnonFunctionType;
        _base.state.noAnonFunctionType = true;
        flowParseTypeAnnotation();
        _base.state.noAnonFunctionType = oldNoAnonFunctionType;
      }
    }
    exports.flowStartParseAsyncArrowFromCallExpression = flowStartParseAsyncArrowFromCallExpression;
    function flowParseMaybeAssign(noIn, isWithinParens) {
      if (_index.match.call(void 0, _types.TokenType.lessThan)) {
        const snapshot = _base.state.snapshot();
        let wasArrow = _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
          _base.state.type = _types.TokenType.typeParameterStart;
        } else {
          return wasArrow;
        }
        const oldIsType = _index.pushTypeContext.call(void 0, 0);
        flowParseTypeParameterDeclaration();
        _index.popTypeContext.call(void 0, oldIsType);
        wasArrow = _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
        if (wasArrow) {
          return true;
        }
        _util.unexpected.call(void 0);
      }
      return _expression.baseParseMaybeAssign.call(void 0, noIn, isWithinParens);
    }
    exports.flowParseMaybeAssign = flowParseMaybeAssign;
    function flowParseArrow() {
      if (_index.match.call(void 0, _types.TokenType.colon)) {
        const oldIsType = _index.pushTypeContext.call(void 0, 0);
        const snapshot = _base.state.snapshot();
        const oldNoAnonFunctionType = _base.state.noAnonFunctionType;
        _base.state.noAnonFunctionType = true;
        flowParseTypeAndPredicateInitialiser();
        _base.state.noAnonFunctionType = oldNoAnonFunctionType;
        if (_util.canInsertSemicolon.call(void 0)) _util.unexpected.call(void 0);
        if (!_index.match.call(void 0, _types.TokenType.arrow)) _util.unexpected.call(void 0);
        if (_base.state.error) {
          _base.state.restoreFromSnapshot(snapshot);
        }
        _index.popTypeContext.call(void 0, oldIsType);
      }
      return _index.eat.call(void 0, _types.TokenType.arrow);
    }
    exports.flowParseArrow = flowParseArrow;
    function flowParseSubscripts(startTokenIndex, noCalls = false) {
      if (_base.state.tokens[_base.state.tokens.length - 1].contextualKeyword === _keywords.ContextualKeyword._async && _index.match.call(void 0, _types.TokenType.lessThan)) {
        const snapshot = _base.state.snapshot();
        const wasArrow = parseAsyncArrowWithTypeParameters();
        if (wasArrow && !_base.state.error) {
          return;
        }
        _base.state.restoreFromSnapshot(snapshot);
      }
      _expression.baseParseSubscripts.call(void 0, startTokenIndex, noCalls);
    }
    exports.flowParseSubscripts = flowParseSubscripts;
    function parseAsyncArrowWithTypeParameters() {
      _base.state.scopeDepth++;
      const startTokenIndex = _base.state.tokens.length;
      _statement.parseFunctionParams.call(void 0);
      if (!_expression.parseArrow.call(void 0)) {
        return false;
      }
      _expression.parseArrowExpression.call(void 0, startTokenIndex);
      return true;
    }
    function flowParseEnumDeclaration() {
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._enum);
      _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._enum;
      _expression.parseIdentifier.call(void 0);
      flowParseEnumBody();
    }
    function flowParseEnumBody() {
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._of)) {
        _index.next.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.braceL);
      flowParseEnumMembers();
      _util.expect.call(void 0, _types.TokenType.braceR);
    }
    function flowParseEnumMembers() {
      while (!_index.match.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (_index.eat.call(void 0, _types.TokenType.ellipsis)) {
          break;
        }
        flowParseEnumMember();
        if (!_index.match.call(void 0, _types.TokenType.braceR)) {
          _util.expect.call(void 0, _types.TokenType.comma);
        }
      }
    }
    function flowParseEnumMember() {
      _expression.parseIdentifier.call(void 0);
      if (_index.eat.call(void 0, _types.TokenType.eq)) {
        _index.next.call(void 0);
      }
    }
  }
});

// node_modules/sucrase/dist/parser/traverser/statement.js
var require_statement = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/statement.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_parser();
    var _flow = require_flow();
    var _typescript = require_typescript();
    var _tokenizer = require_tokenizer();
    var _keywords = require_keywords();
    var _state = require_state();
    var _types = require_types();
    var _charcodes = require_charcodes();
    var _base = require_base();
    var _expression = require_expression();
    var _lval = require_lval();
    var _util = require_util();
    function parseTopLevel() {
      parseBlockBody(_types.TokenType.eof);
      _base.state.scopes.push(new (0, _state.Scope)(0, _base.state.tokens.length, true));
      if (_base.state.scopeDepth !== 0) {
        throw new Error(`Invalid scope depth at end of file: ${_base.state.scopeDepth}`);
      }
      return new (0, _index.File)(_base.state.tokens, _base.state.scopes);
    }
    exports.parseTopLevel = parseTopLevel;
    function parseStatement(declaration) {
      if (_base.isFlowEnabled) {
        if (_flow.flowTryParseStatement.call(void 0)) {
          return;
        }
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.at)) {
        parseDecorators();
      }
      parseStatementContent(declaration);
    }
    exports.parseStatement = parseStatement;
    function parseStatementContent(declaration) {
      if (_base.isTypeScriptEnabled) {
        if (_typescript.tsTryParseStatementContent.call(void 0)) {
          return;
        }
      }
      const starttype = _base.state.type;
      switch (starttype) {
        case _types.TokenType._break:
        case _types.TokenType._continue:
          parseBreakContinueStatement();
          return;
        case _types.TokenType._debugger:
          parseDebuggerStatement();
          return;
        case _types.TokenType._do:
          parseDoStatement();
          return;
        case _types.TokenType._for:
          parseForStatement();
          return;
        case _types.TokenType._function:
          if (_tokenizer.lookaheadType.call(void 0) === _types.TokenType.dot) break;
          if (!declaration) _util.unexpected.call(void 0);
          parseFunctionStatement();
          return;
        case _types.TokenType._class:
          if (!declaration) _util.unexpected.call(void 0);
          parseClass(true);
          return;
        case _types.TokenType._if:
          parseIfStatement();
          return;
        case _types.TokenType._return:
          parseReturnStatement();
          return;
        case _types.TokenType._switch:
          parseSwitchStatement();
          return;
        case _types.TokenType._throw:
          parseThrowStatement();
          return;
        case _types.TokenType._try:
          parseTryStatement();
          return;
        case _types.TokenType._let:
        case _types.TokenType._const:
          if (!declaration) _util.unexpected.call(void 0);
        // NOTE: falls through to _var
        case _types.TokenType._var:
          parseVarStatement(starttype !== _types.TokenType._var);
          return;
        case _types.TokenType._while:
          parseWhileStatement();
          return;
        case _types.TokenType.braceL:
          parseBlock();
          return;
        case _types.TokenType.semi:
          parseEmptyStatement();
          return;
        case _types.TokenType._export:
        case _types.TokenType._import: {
          const nextType = _tokenizer.lookaheadType.call(void 0);
          if (nextType === _types.TokenType.parenL || nextType === _types.TokenType.dot) {
            break;
          }
          _tokenizer.next.call(void 0);
          if (starttype === _types.TokenType._import) {
            parseImport();
          } else {
            parseExport();
          }
          return;
        }
        case _types.TokenType.name:
          if (_base.state.contextualKeyword === _keywords.ContextualKeyword._async) {
            const functionStart = _base.state.start;
            const snapshot = _base.state.snapshot();
            _tokenizer.next.call(void 0);
            if (_tokenizer.match.call(void 0, _types.TokenType._function) && !_util.canInsertSemicolon.call(void 0)) {
              _util.expect.call(void 0, _types.TokenType._function);
              parseFunction(functionStart, true);
              return;
            } else {
              _base.state.restoreFromSnapshot(snapshot);
            }
          } else if (_base.state.contextualKeyword === _keywords.ContextualKeyword._using && !_util.hasFollowingLineBreak.call(void 0) && // Statements like `using[0]` and `using in foo` aren't actual using
          // declarations.
          _tokenizer.lookaheadType.call(void 0) === _types.TokenType.name) {
            parseVarStatement(true);
            return;
          } else if (startsAwaitUsing()) {
            _util.expectContextual.call(void 0, _keywords.ContextualKeyword._await);
            parseVarStatement(true);
            return;
          }
        default:
          break;
      }
      const initialTokensLength = _base.state.tokens.length;
      _expression.parseExpression.call(void 0);
      let simpleName = null;
      if (_base.state.tokens.length === initialTokensLength + 1) {
        const token = _base.state.tokens[_base.state.tokens.length - 1];
        if (token.type === _types.TokenType.name) {
          simpleName = token.contextualKeyword;
        }
      }
      if (simpleName == null) {
        _util.semicolon.call(void 0);
        return;
      }
      if (_tokenizer.eat.call(void 0, _types.TokenType.colon)) {
        parseLabeledStatement();
      } else {
        parseIdentifierStatement(simpleName);
      }
    }
    function startsAwaitUsing() {
      if (!_util.isContextual.call(void 0, _keywords.ContextualKeyword._await)) {
        return false;
      }
      const snapshot = _base.state.snapshot();
      _tokenizer.next.call(void 0);
      if (!_util.isContextual.call(void 0, _keywords.ContextualKeyword._using) || _util.hasPrecedingLineBreak.call(void 0)) {
        _base.state.restoreFromSnapshot(snapshot);
        return false;
      }
      _tokenizer.next.call(void 0);
      if (!_tokenizer.match.call(void 0, _types.TokenType.name) || _util.hasPrecedingLineBreak.call(void 0)) {
        _base.state.restoreFromSnapshot(snapshot);
        return false;
      }
      _base.state.restoreFromSnapshot(snapshot);
      return true;
    }
    function parseDecorators() {
      while (_tokenizer.match.call(void 0, _types.TokenType.at)) {
        parseDecorator();
      }
    }
    exports.parseDecorators = parseDecorators;
    function parseDecorator() {
      _tokenizer.next.call(void 0);
      if (_tokenizer.eat.call(void 0, _types.TokenType.parenL)) {
        _expression.parseExpression.call(void 0);
        _util.expect.call(void 0, _types.TokenType.parenR);
      } else {
        _expression.parseIdentifier.call(void 0);
        while (_tokenizer.eat.call(void 0, _types.TokenType.dot)) {
          _expression.parseIdentifier.call(void 0);
        }
        parseMaybeDecoratorArguments();
      }
    }
    function parseMaybeDecoratorArguments() {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseMaybeDecoratorArguments.call(void 0);
      } else {
        baseParseMaybeDecoratorArguments();
      }
    }
    function baseParseMaybeDecoratorArguments() {
      if (_tokenizer.eat.call(void 0, _types.TokenType.parenL)) {
        _expression.parseCallExpressionArguments.call(void 0);
      }
    }
    exports.baseParseMaybeDecoratorArguments = baseParseMaybeDecoratorArguments;
    function parseBreakContinueStatement() {
      _tokenizer.next.call(void 0);
      if (!_util.isLineTerminator.call(void 0)) {
        _expression.parseIdentifier.call(void 0);
        _util.semicolon.call(void 0);
      }
    }
    function parseDebuggerStatement() {
      _tokenizer.next.call(void 0);
      _util.semicolon.call(void 0);
    }
    function parseDoStatement() {
      _tokenizer.next.call(void 0);
      parseStatement(false);
      _util.expect.call(void 0, _types.TokenType._while);
      _expression.parseParenExpression.call(void 0);
      _tokenizer.eat.call(void 0, _types.TokenType.semi);
    }
    function parseForStatement() {
      _base.state.scopeDepth++;
      const startTokenIndex = _base.state.tokens.length;
      parseAmbiguousForStatement();
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, false));
      _base.state.scopeDepth--;
    }
    function isUsingInLoop() {
      if (!_util.isContextual.call(void 0, _keywords.ContextualKeyword._using)) {
        return false;
      }
      if (_util.isLookaheadContextual.call(void 0, _keywords.ContextualKeyword._of)) {
        return false;
      }
      return true;
    }
    function parseAmbiguousForStatement() {
      _tokenizer.next.call(void 0);
      let forAwait = false;
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._await)) {
        forAwait = true;
        _tokenizer.next.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.parenL);
      if (_tokenizer.match.call(void 0, _types.TokenType.semi)) {
        if (forAwait) {
          _util.unexpected.call(void 0);
        }
        parseFor();
        return;
      }
      const isAwaitUsing = startsAwaitUsing();
      if (isAwaitUsing || _tokenizer.match.call(void 0, _types.TokenType._var) || _tokenizer.match.call(void 0, _types.TokenType._let) || _tokenizer.match.call(void 0, _types.TokenType._const) || isUsingInLoop()) {
        if (isAwaitUsing) {
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._await);
        }
        _tokenizer.next.call(void 0);
        parseVar(true, _base.state.type !== _types.TokenType._var);
        if (_tokenizer.match.call(void 0, _types.TokenType._in) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._of)) {
          parseForIn(forAwait);
          return;
        }
        parseFor();
        return;
      }
      _expression.parseExpression.call(void 0, true);
      if (_tokenizer.match.call(void 0, _types.TokenType._in) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._of)) {
        parseForIn(forAwait);
        return;
      }
      if (forAwait) {
        _util.unexpected.call(void 0);
      }
      parseFor();
    }
    function parseFunctionStatement() {
      const functionStart = _base.state.start;
      _tokenizer.next.call(void 0);
      parseFunction(functionStart, true);
    }
    function parseIfStatement() {
      _tokenizer.next.call(void 0);
      _expression.parseParenExpression.call(void 0);
      parseStatement(false);
      if (_tokenizer.eat.call(void 0, _types.TokenType._else)) {
        parseStatement(false);
      }
    }
    function parseReturnStatement() {
      _tokenizer.next.call(void 0);
      if (!_util.isLineTerminator.call(void 0)) {
        _expression.parseExpression.call(void 0);
        _util.semicolon.call(void 0);
      }
    }
    function parseSwitchStatement() {
      _tokenizer.next.call(void 0);
      _expression.parseParenExpression.call(void 0);
      _base.state.scopeDepth++;
      const startTokenIndex = _base.state.tokens.length;
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_tokenizer.match.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (_tokenizer.match.call(void 0, _types.TokenType._case) || _tokenizer.match.call(void 0, _types.TokenType._default)) {
          const isCase = _tokenizer.match.call(void 0, _types.TokenType._case);
          _tokenizer.next.call(void 0);
          if (isCase) {
            _expression.parseExpression.call(void 0);
          }
          _util.expect.call(void 0, _types.TokenType.colon);
        } else {
          parseStatement(true);
        }
      }
      _tokenizer.next.call(void 0);
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, false));
      _base.state.scopeDepth--;
    }
    function parseThrowStatement() {
      _tokenizer.next.call(void 0);
      _expression.parseExpression.call(void 0);
      _util.semicolon.call(void 0);
    }
    function parseCatchClauseParam() {
      _lval.parseBindingAtom.call(
        void 0,
        true
        /* isBlockScope */
      );
      if (_base.isTypeScriptEnabled) {
        _typescript.tsTryParseTypeAnnotation.call(void 0);
      }
    }
    function parseTryStatement() {
      _tokenizer.next.call(void 0);
      parseBlock();
      if (_tokenizer.match.call(void 0, _types.TokenType._catch)) {
        _tokenizer.next.call(void 0);
        let catchBindingStartTokenIndex = null;
        if (_tokenizer.match.call(void 0, _types.TokenType.parenL)) {
          _base.state.scopeDepth++;
          catchBindingStartTokenIndex = _base.state.tokens.length;
          _util.expect.call(void 0, _types.TokenType.parenL);
          parseCatchClauseParam();
          _util.expect.call(void 0, _types.TokenType.parenR);
        }
        parseBlock();
        if (catchBindingStartTokenIndex != null) {
          const endTokenIndex = _base.state.tokens.length;
          _base.state.scopes.push(new (0, _state.Scope)(catchBindingStartTokenIndex, endTokenIndex, false));
          _base.state.scopeDepth--;
        }
      }
      if (_tokenizer.eat.call(void 0, _types.TokenType._finally)) {
        parseBlock();
      }
    }
    function parseVarStatement(isBlockScope) {
      _tokenizer.next.call(void 0);
      parseVar(false, isBlockScope);
      _util.semicolon.call(void 0);
    }
    exports.parseVarStatement = parseVarStatement;
    function parseWhileStatement() {
      _tokenizer.next.call(void 0);
      _expression.parseParenExpression.call(void 0);
      parseStatement(false);
    }
    function parseEmptyStatement() {
      _tokenizer.next.call(void 0);
    }
    function parseLabeledStatement() {
      parseStatement(true);
    }
    function parseIdentifierStatement(contextualKeyword) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseIdentifierStatement.call(void 0, contextualKeyword);
      } else if (_base.isFlowEnabled) {
        _flow.flowParseIdentifierStatement.call(void 0, contextualKeyword);
      } else {
        _util.semicolon.call(void 0);
      }
    }
    function parseBlock(isFunctionScope = false, contextId = 0) {
      const startTokenIndex = _base.state.tokens.length;
      _base.state.scopeDepth++;
      _util.expect.call(void 0, _types.TokenType.braceL);
      if (contextId) {
        _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
      }
      parseBlockBody(_types.TokenType.braceR);
      if (contextId) {
        _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
      }
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, isFunctionScope));
      _base.state.scopeDepth--;
    }
    exports.parseBlock = parseBlock;
    function parseBlockBody(end) {
      while (!_tokenizer.eat.call(void 0, end) && !_base.state.error) {
        parseStatement(true);
      }
    }
    exports.parseBlockBody = parseBlockBody;
    function parseFor() {
      _util.expect.call(void 0, _types.TokenType.semi);
      if (!_tokenizer.match.call(void 0, _types.TokenType.semi)) {
        _expression.parseExpression.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.semi);
      if (!_tokenizer.match.call(void 0, _types.TokenType.parenR)) {
        _expression.parseExpression.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.parenR);
      parseStatement(false);
    }
    function parseForIn(forAwait) {
      if (forAwait) {
        _util.eatContextual.call(void 0, _keywords.ContextualKeyword._of);
      } else {
        _tokenizer.next.call(void 0);
      }
      _expression.parseExpression.call(void 0);
      _util.expect.call(void 0, _types.TokenType.parenR);
      parseStatement(false);
    }
    function parseVar(isFor, isBlockScope) {
      while (true) {
        parseVarHead(isBlockScope);
        if (_tokenizer.eat.call(void 0, _types.TokenType.eq)) {
          const eqIndex = _base.state.tokens.length - 1;
          _expression.parseMaybeAssign.call(void 0, isFor);
          _base.state.tokens[eqIndex].rhsEndIndex = _base.state.tokens.length;
        }
        if (!_tokenizer.eat.call(void 0, _types.TokenType.comma)) {
          break;
        }
      }
    }
    function parseVarHead(isBlockScope) {
      _lval.parseBindingAtom.call(void 0, isBlockScope);
      if (_base.isTypeScriptEnabled) {
        _typescript.tsAfterParseVarHead.call(void 0);
      } else if (_base.isFlowEnabled) {
        _flow.flowAfterParseVarHead.call(void 0);
      }
    }
    function parseFunction(functionStart, isStatement, optionalId = false) {
      if (_tokenizer.match.call(void 0, _types.TokenType.star)) {
        _tokenizer.next.call(void 0);
      }
      if (isStatement && !optionalId && !_tokenizer.match.call(void 0, _types.TokenType.name) && !_tokenizer.match.call(void 0, _types.TokenType._yield)) {
        _util.unexpected.call(void 0);
      }
      let nameScopeStartTokenIndex = null;
      if (_tokenizer.match.call(void 0, _types.TokenType.name)) {
        if (!isStatement) {
          nameScopeStartTokenIndex = _base.state.tokens.length;
          _base.state.scopeDepth++;
        }
        _lval.parseBindingIdentifier.call(void 0, false);
      }
      const startTokenIndex = _base.state.tokens.length;
      _base.state.scopeDepth++;
      parseFunctionParams();
      _expression.parseFunctionBodyAndFinish.call(void 0, functionStart);
      const endTokenIndex = _base.state.tokens.length;
      _base.state.scopes.push(new (0, _state.Scope)(startTokenIndex, endTokenIndex, true));
      _base.state.scopeDepth--;
      if (nameScopeStartTokenIndex !== null) {
        _base.state.scopes.push(new (0, _state.Scope)(nameScopeStartTokenIndex, endTokenIndex, true));
        _base.state.scopeDepth--;
      }
    }
    exports.parseFunction = parseFunction;
    function parseFunctionParams(allowModifiers = false, funcContextId = 0) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsStartParseFunctionParams.call(void 0);
      } else if (_base.isFlowEnabled) {
        _flow.flowStartParseFunctionParams.call(void 0);
      }
      _util.expect.call(void 0, _types.TokenType.parenL);
      if (funcContextId) {
        _base.state.tokens[_base.state.tokens.length - 1].contextId = funcContextId;
      }
      _lval.parseBindingList.call(
        void 0,
        _types.TokenType.parenR,
        false,
        false,
        allowModifiers,
        funcContextId
      );
      if (funcContextId) {
        _base.state.tokens[_base.state.tokens.length - 1].contextId = funcContextId;
      }
    }
    exports.parseFunctionParams = parseFunctionParams;
    function parseClass(isStatement, optionalId = false) {
      const contextId = _base.getNextContextId.call(void 0);
      _tokenizer.next.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
      _base.state.tokens[_base.state.tokens.length - 1].isExpression = !isStatement;
      let nameScopeStartTokenIndex = null;
      if (!isStatement) {
        nameScopeStartTokenIndex = _base.state.tokens.length;
        _base.state.scopeDepth++;
      }
      parseClassId(isStatement, optionalId);
      parseClassSuper();
      const openBraceIndex = _base.state.tokens.length;
      parseClassBody(contextId);
      if (_base.state.error) {
        return;
      }
      _base.state.tokens[openBraceIndex].contextId = contextId;
      _base.state.tokens[_base.state.tokens.length - 1].contextId = contextId;
      if (nameScopeStartTokenIndex !== null) {
        const endTokenIndex = _base.state.tokens.length;
        _base.state.scopes.push(new (0, _state.Scope)(nameScopeStartTokenIndex, endTokenIndex, false));
        _base.state.scopeDepth--;
      }
    }
    exports.parseClass = parseClass;
    function isClassProperty() {
      return _tokenizer.match.call(void 0, _types.TokenType.eq) || _tokenizer.match.call(void 0, _types.TokenType.semi) || _tokenizer.match.call(void 0, _types.TokenType.braceR) || _tokenizer.match.call(void 0, _types.TokenType.bang) || _tokenizer.match.call(void 0, _types.TokenType.colon);
    }
    function isClassMethod() {
      return _tokenizer.match.call(void 0, _types.TokenType.parenL) || _tokenizer.match.call(void 0, _types.TokenType.lessThan);
    }
    function parseClassBody(classContextId) {
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_tokenizer.eat.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (_tokenizer.eat.call(void 0, _types.TokenType.semi)) {
          continue;
        }
        if (_tokenizer.match.call(void 0, _types.TokenType.at)) {
          parseDecorator();
          continue;
        }
        const memberStart = _base.state.start;
        parseClassMember(memberStart, classContextId);
      }
    }
    function parseClassMember(memberStart, classContextId) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseModifiers.call(void 0, [
          _keywords.ContextualKeyword._declare,
          _keywords.ContextualKeyword._public,
          _keywords.ContextualKeyword._protected,
          _keywords.ContextualKeyword._private,
          _keywords.ContextualKeyword._override
        ]);
      }
      let isStatic = false;
      if (_tokenizer.match.call(void 0, _types.TokenType.name) && _base.state.contextualKeyword === _keywords.ContextualKeyword._static) {
        _expression.parseIdentifier.call(void 0);
        if (isClassMethod()) {
          parseClassMethod(
            memberStart,
            /* isConstructor */
            false
          );
          return;
        } else if (isClassProperty()) {
          parseClassProperty();
          return;
        }
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._static;
        isStatic = true;
        if (_tokenizer.match.call(void 0, _types.TokenType.braceL)) {
          _base.state.tokens[_base.state.tokens.length - 1].contextId = classContextId;
          parseBlock();
          return;
        }
      }
      parseClassMemberWithIsStatic(memberStart, isStatic, classContextId);
    }
    function parseClassMemberWithIsStatic(memberStart, isStatic, classContextId) {
      if (_base.isTypeScriptEnabled) {
        if (_typescript.tsTryParseClassMemberWithIsStatic.call(void 0, isStatic)) {
          return;
        }
      }
      if (_tokenizer.eat.call(void 0, _types.TokenType.star)) {
        parseClassPropertyName(classContextId);
        parseClassMethod(
          memberStart,
          /* isConstructor */
          false
        );
        return;
      }
      parseClassPropertyName(classContextId);
      let isConstructor = false;
      const token = _base.state.tokens[_base.state.tokens.length - 1];
      if (token.contextualKeyword === _keywords.ContextualKeyword._constructor) {
        isConstructor = true;
      }
      parsePostMemberNameModifiers();
      if (isClassMethod()) {
        parseClassMethod(memberStart, isConstructor);
      } else if (isClassProperty()) {
        parseClassProperty();
      } else if (token.contextualKeyword === _keywords.ContextualKeyword._async && !_util.isLineTerminator.call(void 0)) {
        _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._async;
        const isGenerator = _tokenizer.match.call(void 0, _types.TokenType.star);
        if (isGenerator) {
          _tokenizer.next.call(void 0);
        }
        parseClassPropertyName(classContextId);
        parsePostMemberNameModifiers();
        parseClassMethod(
          memberStart,
          false
          /* isConstructor */
        );
      } else if ((token.contextualKeyword === _keywords.ContextualKeyword._get || token.contextualKeyword === _keywords.ContextualKeyword._set) && !(_util.isLineTerminator.call(void 0) && _tokenizer.match.call(void 0, _types.TokenType.star))) {
        if (token.contextualKeyword === _keywords.ContextualKeyword._get) {
          _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._get;
        } else {
          _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._set;
        }
        parseClassPropertyName(classContextId);
        parseClassMethod(
          memberStart,
          /* isConstructor */
          false
        );
      } else if (token.contextualKeyword === _keywords.ContextualKeyword._accessor && !_util.isLineTerminator.call(void 0)) {
        parseClassPropertyName(classContextId);
        parseClassProperty();
      } else if (_util.isLineTerminator.call(void 0)) {
        parseClassProperty();
      } else {
        _util.unexpected.call(void 0);
      }
    }
    function parseClassMethod(functionStart, isConstructor) {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsTryParseTypeParameters.call(void 0);
      } else if (_base.isFlowEnabled) {
        if (_tokenizer.match.call(void 0, _types.TokenType.lessThan)) {
          _flow.flowParseTypeParameterDeclaration.call(void 0);
        }
      }
      _expression.parseMethod.call(void 0, functionStart, isConstructor);
    }
    function parseClassPropertyName(classContextId) {
      _expression.parsePropertyName.call(void 0, classContextId);
    }
    exports.parseClassPropertyName = parseClassPropertyName;
    function parsePostMemberNameModifiers() {
      if (_base.isTypeScriptEnabled) {
        const oldIsType = _tokenizer.pushTypeContext.call(void 0, 0);
        _tokenizer.eat.call(void 0, _types.TokenType.question);
        _tokenizer.popTypeContext.call(void 0, oldIsType);
      }
    }
    exports.parsePostMemberNameModifiers = parsePostMemberNameModifiers;
    function parseClassProperty() {
      if (_base.isTypeScriptEnabled) {
        _tokenizer.eatTypeToken.call(void 0, _types.TokenType.bang);
        _typescript.tsTryParseTypeAnnotation.call(void 0);
      } else if (_base.isFlowEnabled) {
        if (_tokenizer.match.call(void 0, _types.TokenType.colon)) {
          _flow.flowParseTypeAnnotation.call(void 0);
        }
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.eq)) {
        const equalsTokenIndex = _base.state.tokens.length;
        _tokenizer.next.call(void 0);
        _expression.parseMaybeAssign.call(void 0);
        _base.state.tokens[equalsTokenIndex].rhsEndIndex = _base.state.tokens.length;
      }
      _util.semicolon.call(void 0);
    }
    exports.parseClassProperty = parseClassProperty;
    function parseClassId(isStatement, optionalId = false) {
      if (_base.isTypeScriptEnabled && (!isStatement || optionalId) && _util.isContextual.call(void 0, _keywords.ContextualKeyword._implements)) {
        return;
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.name)) {
        _lval.parseBindingIdentifier.call(void 0, true);
      }
      if (_base.isTypeScriptEnabled) {
        _typescript.tsTryParseTypeParameters.call(void 0);
      } else if (_base.isFlowEnabled) {
        if (_tokenizer.match.call(void 0, _types.TokenType.lessThan)) {
          _flow.flowParseTypeParameterDeclaration.call(void 0);
        }
      }
    }
    function parseClassSuper() {
      let hasSuper = false;
      if (_tokenizer.eat.call(void 0, _types.TokenType._extends)) {
        _expression.parseExprSubscripts.call(void 0);
        hasSuper = true;
      } else {
        hasSuper = false;
      }
      if (_base.isTypeScriptEnabled) {
        _typescript.tsAfterParseClassSuper.call(void 0, hasSuper);
      } else if (_base.isFlowEnabled) {
        _flow.flowAfterParseClassSuper.call(void 0, hasSuper);
      }
    }
    function parseExport() {
      const exportIndex = _base.state.tokens.length - 1;
      if (_base.isTypeScriptEnabled) {
        if (_typescript.tsTryParseExport.call(void 0)) {
          return;
        }
      }
      if (shouldParseExportStar()) {
        parseExportStar();
      } else if (isExportDefaultSpecifier()) {
        _expression.parseIdentifier.call(void 0);
        if (_tokenizer.match.call(void 0, _types.TokenType.comma) && _tokenizer.lookaheadType.call(void 0) === _types.TokenType.star) {
          _util.expect.call(void 0, _types.TokenType.comma);
          _util.expect.call(void 0, _types.TokenType.star);
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._as);
          _expression.parseIdentifier.call(void 0);
        } else {
          parseExportSpecifiersMaybe();
        }
        parseExportFrom();
      } else if (_tokenizer.eat.call(void 0, _types.TokenType._default)) {
        parseExportDefaultExpression();
      } else if (shouldParseExportDeclaration()) {
        parseExportDeclaration();
      } else {
        parseExportSpecifiers();
        parseExportFrom();
      }
      _base.state.tokens[exportIndex].rhsEndIndex = _base.state.tokens.length;
    }
    exports.parseExport = parseExport;
    function parseExportDefaultExpression() {
      if (_base.isTypeScriptEnabled) {
        if (_typescript.tsTryParseExportDefaultExpression.call(void 0)) {
          return;
        }
      }
      if (_base.isFlowEnabled) {
        if (_flow.flowTryParseExportDefaultExpression.call(void 0)) {
          return;
        }
      }
      const functionStart = _base.state.start;
      if (_tokenizer.eat.call(void 0, _types.TokenType._function)) {
        parseFunction(functionStart, true, true);
      } else if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._async) && _tokenizer.lookaheadType.call(void 0) === _types.TokenType._function) {
        _util.eatContextual.call(void 0, _keywords.ContextualKeyword._async);
        _tokenizer.eat.call(void 0, _types.TokenType._function);
        parseFunction(functionStart, true, true);
      } else if (_tokenizer.match.call(void 0, _types.TokenType._class)) {
        parseClass(true, true);
      } else if (_tokenizer.match.call(void 0, _types.TokenType.at)) {
        parseDecorators();
        parseClass(true, true);
      } else {
        _expression.parseMaybeAssign.call(void 0);
        _util.semicolon.call(void 0);
      }
    }
    function parseExportDeclaration() {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseExportDeclaration.call(void 0);
      } else if (_base.isFlowEnabled) {
        _flow.flowParseExportDeclaration.call(void 0);
      } else {
        parseStatement(true);
      }
    }
    function isExportDefaultSpecifier() {
      if (_base.isTypeScriptEnabled && _typescript.tsIsDeclarationStart.call(void 0)) {
        return false;
      } else if (_base.isFlowEnabled && _flow.flowShouldDisallowExportDefaultSpecifier.call(void 0)) {
        return false;
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.name)) {
        return _base.state.contextualKeyword !== _keywords.ContextualKeyword._async;
      }
      if (!_tokenizer.match.call(void 0, _types.TokenType._default)) {
        return false;
      }
      const _next = _tokenizer.nextTokenStart.call(void 0);
      const lookahead2 = _tokenizer.lookaheadTypeAndKeyword.call(void 0);
      const hasFrom = lookahead2.type === _types.TokenType.name && lookahead2.contextualKeyword === _keywords.ContextualKeyword._from;
      if (lookahead2.type === _types.TokenType.comma) {
        return true;
      }
      if (hasFrom) {
        const nextAfterFrom = _base.input.charCodeAt(_tokenizer.nextTokenStartSince.call(void 0, _next + 4));
        return nextAfterFrom === _charcodes.charCodes.quotationMark || nextAfterFrom === _charcodes.charCodes.apostrophe;
      }
      return false;
    }
    function parseExportSpecifiersMaybe() {
      if (_tokenizer.eat.call(void 0, _types.TokenType.comma)) {
        parseExportSpecifiers();
      }
    }
    function parseExportFrom() {
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._from)) {
        _expression.parseExprAtom.call(void 0);
        maybeParseImportAttributes();
      }
      _util.semicolon.call(void 0);
    }
    exports.parseExportFrom = parseExportFrom;
    function shouldParseExportStar() {
      if (_base.isFlowEnabled) {
        return _flow.flowShouldParseExportStar.call(void 0);
      } else {
        return _tokenizer.match.call(void 0, _types.TokenType.star);
      }
    }
    function parseExportStar() {
      if (_base.isFlowEnabled) {
        _flow.flowParseExportStar.call(void 0);
      } else {
        baseParseExportStar();
      }
    }
    function baseParseExportStar() {
      _util.expect.call(void 0, _types.TokenType.star);
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        parseExportNamespace();
      } else {
        parseExportFrom();
      }
    }
    exports.baseParseExportStar = baseParseExportStar;
    function parseExportNamespace() {
      _tokenizer.next.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].type = _types.TokenType._as;
      _expression.parseIdentifier.call(void 0);
      parseExportSpecifiersMaybe();
      parseExportFrom();
    }
    function shouldParseExportDeclaration() {
      return _base.isTypeScriptEnabled && _typescript.tsIsDeclarationStart.call(void 0) || _base.isFlowEnabled && _flow.flowShouldParseExportDeclaration.call(void 0) || _base.state.type === _types.TokenType._var || _base.state.type === _types.TokenType._const || _base.state.type === _types.TokenType._let || _base.state.type === _types.TokenType._function || _base.state.type === _types.TokenType._class || _util.isContextual.call(void 0, _keywords.ContextualKeyword._async) || _tokenizer.match.call(void 0, _types.TokenType.at);
    }
    function parseExportSpecifiers() {
      let first = true;
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_tokenizer.eat.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          _util.expect.call(void 0, _types.TokenType.comma);
          if (_tokenizer.eat.call(void 0, _types.TokenType.braceR)) {
            break;
          }
        }
        parseExportSpecifier();
      }
    }
    exports.parseExportSpecifiers = parseExportSpecifiers;
    function parseExportSpecifier() {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseExportSpecifier.call(void 0);
        return;
      }
      _expression.parseIdentifier.call(void 0);
      _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _tokenizer.IdentifierRole.ExportAccess;
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        _expression.parseIdentifier.call(void 0);
      }
    }
    function isImportReflection() {
      const snapshot = _base.state.snapshot();
      _util.expectContextual.call(void 0, _keywords.ContextualKeyword._module);
      if (_util.eatContextual.call(void 0, _keywords.ContextualKeyword._from)) {
        if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._from)) {
          _base.state.restoreFromSnapshot(snapshot);
          return true;
        } else {
          _base.state.restoreFromSnapshot(snapshot);
          return false;
        }
      } else if (_tokenizer.match.call(void 0, _types.TokenType.comma)) {
        _base.state.restoreFromSnapshot(snapshot);
        return false;
      } else {
        _base.state.restoreFromSnapshot(snapshot);
        return true;
      }
    }
    function parseMaybeImportReflection() {
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._module) && isImportReflection()) {
        _tokenizer.next.call(void 0);
      }
    }
    function parseImport() {
      if (_base.isTypeScriptEnabled && _tokenizer.match.call(void 0, _types.TokenType.name) && _tokenizer.lookaheadType.call(void 0) === _types.TokenType.eq) {
        _typescript.tsParseImportEqualsDeclaration.call(void 0);
        return;
      }
      if (_base.isTypeScriptEnabled && _util.isContextual.call(void 0, _keywords.ContextualKeyword._type)) {
        const lookahead2 = _tokenizer.lookaheadTypeAndKeyword.call(void 0);
        if (lookahead2.type === _types.TokenType.name && lookahead2.contextualKeyword !== _keywords.ContextualKeyword._from) {
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._type);
          if (_tokenizer.lookaheadType.call(void 0) === _types.TokenType.eq) {
            _typescript.tsParseImportEqualsDeclaration.call(void 0);
            return;
          }
        } else if (lookahead2.type === _types.TokenType.star || lookahead2.type === _types.TokenType.braceL) {
          _util.expectContextual.call(void 0, _keywords.ContextualKeyword._type);
        }
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.string)) {
        _expression.parseExprAtom.call(void 0);
      } else {
        parseMaybeImportReflection();
        parseImportSpecifiers();
        _util.expectContextual.call(void 0, _keywords.ContextualKeyword._from);
        _expression.parseExprAtom.call(void 0);
      }
      maybeParseImportAttributes();
      _util.semicolon.call(void 0);
    }
    exports.parseImport = parseImport;
    function shouldParseDefaultImport() {
      return _tokenizer.match.call(void 0, _types.TokenType.name);
    }
    function parseImportSpecifierLocal() {
      _lval.parseImportedIdentifier.call(void 0);
    }
    function parseImportSpecifiers() {
      if (_base.isFlowEnabled) {
        _flow.flowStartParseImportSpecifiers.call(void 0);
      }
      let first = true;
      if (shouldParseDefaultImport()) {
        parseImportSpecifierLocal();
        if (!_tokenizer.eat.call(void 0, _types.TokenType.comma)) return;
      }
      if (_tokenizer.match.call(void 0, _types.TokenType.star)) {
        _tokenizer.next.call(void 0);
        _util.expectContextual.call(void 0, _keywords.ContextualKeyword._as);
        parseImportSpecifierLocal();
        return;
      }
      _util.expect.call(void 0, _types.TokenType.braceL);
      while (!_tokenizer.eat.call(void 0, _types.TokenType.braceR) && !_base.state.error) {
        if (first) {
          first = false;
        } else {
          if (_tokenizer.eat.call(void 0, _types.TokenType.colon)) {
            _util.unexpected.call(
              void 0,
              "ES2015 named imports do not destructure. Use another statement for destructuring after the import."
            );
          }
          _util.expect.call(void 0, _types.TokenType.comma);
          if (_tokenizer.eat.call(void 0, _types.TokenType.braceR)) {
            break;
          }
        }
        parseImportSpecifier();
      }
    }
    function parseImportSpecifier() {
      if (_base.isTypeScriptEnabled) {
        _typescript.tsParseImportSpecifier.call(void 0);
        return;
      }
      if (_base.isFlowEnabled) {
        _flow.flowParseImportSpecifier.call(void 0);
        return;
      }
      _lval.parseImportedIdentifier.call(void 0);
      if (_util.isContextual.call(void 0, _keywords.ContextualKeyword._as)) {
        _base.state.tokens[_base.state.tokens.length - 1].identifierRole = _tokenizer.IdentifierRole.ImportAccess;
        _tokenizer.next.call(void 0);
        _lval.parseImportedIdentifier.call(void 0);
      }
    }
    function maybeParseImportAttributes() {
      if (_tokenizer.match.call(void 0, _types.TokenType._with) || _util.isContextual.call(void 0, _keywords.ContextualKeyword._assert) && !_util.hasPrecedingLineBreak.call(void 0)) {
        _tokenizer.next.call(void 0);
        _expression.parseObj.call(void 0, false, false);
      }
    }
  }
});

// node_modules/sucrase/dist/parser/traverser/index.js
var require_traverser = __commonJS({
  "node_modules/sucrase/dist/parser/traverser/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _index = require_tokenizer();
    var _charcodes = require_charcodes();
    var _base = require_base();
    var _statement = require_statement();
    function parseFile() {
      if (_base.state.pos === 0 && _base.input.charCodeAt(0) === _charcodes.charCodes.numberSign && _base.input.charCodeAt(1) === _charcodes.charCodes.exclamationMark) {
        _index.skipLineComment.call(void 0, 2);
      }
      _index.nextToken.call(void 0);
      return _statement.parseTopLevel.call(void 0);
    }
    exports.parseFile = parseFile;
  }
});

// node_modules/sucrase/dist/parser/index.js
var require_parser = __commonJS({
  "node_modules/sucrase/dist/parser/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _base = require_base();
    var _index = require_traverser();
    var File = class {
      constructor(tokens, scopes) {
        this.tokens = tokens;
        this.scopes = scopes;
      }
    };
    exports.File = File;
    function parse4(input, isJSXEnabled, isTypeScriptEnabled, isFlowEnabled) {
      if (isFlowEnabled && isTypeScriptEnabled) {
        throw new Error("Cannot combine flow and typescript plugins.");
      }
      _base.initParser.call(void 0, input, isJSXEnabled, isTypeScriptEnabled, isFlowEnabled);
      const result = _index.parseFile.call(void 0);
      if (_base.state.error) {
        throw _base.augmentError.call(void 0, _base.state.error);
      }
      return result;
    }
    exports.parse = parse4;
  }
});

// node_modules/sucrase/dist/util/isAsyncOperation.js
var require_isAsyncOperation = __commonJS({
  "node_modules/sucrase/dist/util/isAsyncOperation.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    function isAsyncOperation(tokens) {
      let index = tokens.currentIndex();
      let depth = 0;
      const startToken = tokens.currentToken();
      do {
        const token = tokens.tokens[index];
        if (token.isOptionalChainStart) {
          depth++;
        }
        if (token.isOptionalChainEnd) {
          depth--;
        }
        depth += token.numNullishCoalesceStarts;
        depth -= token.numNullishCoalesceEnds;
        if (token.contextualKeyword === _keywords.ContextualKeyword._await && token.identifierRole == null && token.scopeDepth === startToken.scopeDepth) {
          return true;
        }
        index += 1;
      } while (depth > 0 && index < tokens.tokens.length);
      return false;
    }
    exports.default = isAsyncOperation;
  }
});

// node_modules/sucrase/dist/TokenProcessor.js
var require_TokenProcessor = __commonJS({
  "node_modules/sucrase/dist/TokenProcessor.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _isAsyncOperation = require_isAsyncOperation();
    var _isAsyncOperation2 = _interopRequireDefault(_isAsyncOperation);
    var TokenProcessor = class _TokenProcessor {
      __init() {
        this.resultCode = "";
      }
      // Array mapping input token index to optional string index position in the
      // output code.
      __init2() {
        this.resultMappings = new Array(this.tokens.length);
      }
      __init3() {
        this.tokenIndex = 0;
      }
      constructor(code, tokens, isFlowEnabled, disableESTransforms, helperManager) {
        ;
        this.code = code;
        this.tokens = tokens;
        this.isFlowEnabled = isFlowEnabled;
        this.disableESTransforms = disableESTransforms;
        this.helperManager = helperManager;
        _TokenProcessor.prototype.__init.call(this);
        _TokenProcessor.prototype.__init2.call(this);
        _TokenProcessor.prototype.__init3.call(this);
      }
      /**
       * Snapshot the token state in a way that can be restored later, useful for
       * things like lookahead.
       *
       * resultMappings do not need to be copied since in all use cases, they will
       * be overwritten anyway after restore.
       */
      snapshot() {
        return {
          resultCode: this.resultCode,
          tokenIndex: this.tokenIndex
        };
      }
      restoreToSnapshot(snapshot) {
        this.resultCode = snapshot.resultCode;
        this.tokenIndex = snapshot.tokenIndex;
      }
      /**
       * Remove and return the code generated since the snapshot, leaving the
       * current token position in-place. Unlike most TokenProcessor operations,
       * this operation can result in input/output line number mismatches because
       * the removed code may contain newlines, so this operation should be used
       * sparingly.
       */
      dangerouslyGetAndRemoveCodeSinceSnapshot(snapshot) {
        const result = this.resultCode.slice(snapshot.resultCode.length);
        this.resultCode = snapshot.resultCode;
        return result;
      }
      reset() {
        this.resultCode = "";
        this.resultMappings = new Array(this.tokens.length);
        this.tokenIndex = 0;
      }
      matchesContextualAtIndex(index, contextualKeyword) {
        return this.matches1AtIndex(index, _types.TokenType.name) && this.tokens[index].contextualKeyword === contextualKeyword;
      }
      identifierNameAtIndex(index) {
        return this.identifierNameForToken(this.tokens[index]);
      }
      identifierNameAtRelativeIndex(relativeIndex) {
        return this.identifierNameForToken(this.tokenAtRelativeIndex(relativeIndex));
      }
      identifierName() {
        return this.identifierNameForToken(this.currentToken());
      }
      identifierNameForToken(token) {
        return this.code.slice(token.start, token.end);
      }
      rawCodeForToken(token) {
        return this.code.slice(token.start, token.end);
      }
      stringValueAtIndex(index) {
        return this.stringValueForToken(this.tokens[index]);
      }
      stringValue() {
        return this.stringValueForToken(this.currentToken());
      }
      stringValueForToken(token) {
        return this.code.slice(token.start + 1, token.end - 1);
      }
      matches1AtIndex(index, t1) {
        return this.tokens[index].type === t1;
      }
      matches2AtIndex(index, t1, t2) {
        return this.tokens[index].type === t1 && this.tokens[index + 1].type === t2;
      }
      matches3AtIndex(index, t1, t2, t3) {
        return this.tokens[index].type === t1 && this.tokens[index + 1].type === t2 && this.tokens[index + 2].type === t3;
      }
      matches1(t1) {
        return this.tokens[this.tokenIndex].type === t1;
      }
      matches2(t1, t2) {
        return this.tokens[this.tokenIndex].type === t1 && this.tokens[this.tokenIndex + 1].type === t2;
      }
      matches3(t1, t2, t3) {
        return this.tokens[this.tokenIndex].type === t1 && this.tokens[this.tokenIndex + 1].type === t2 && this.tokens[this.tokenIndex + 2].type === t3;
      }
      matches4(t1, t2, t3, t4) {
        return this.tokens[this.tokenIndex].type === t1 && this.tokens[this.tokenIndex + 1].type === t2 && this.tokens[this.tokenIndex + 2].type === t3 && this.tokens[this.tokenIndex + 3].type === t4;
      }
      matches5(t1, t2, t3, t4, t5) {
        return this.tokens[this.tokenIndex].type === t1 && this.tokens[this.tokenIndex + 1].type === t2 && this.tokens[this.tokenIndex + 2].type === t3 && this.tokens[this.tokenIndex + 3].type === t4 && this.tokens[this.tokenIndex + 4].type === t5;
      }
      matchesContextual(contextualKeyword) {
        return this.matchesContextualAtIndex(this.tokenIndex, contextualKeyword);
      }
      matchesContextIdAndLabel(type, contextId) {
        return this.matches1(type) && this.currentToken().contextId === contextId;
      }
      previousWhitespaceAndComments() {
        let whitespaceAndComments = this.code.slice(
          this.tokenIndex > 0 ? this.tokens[this.tokenIndex - 1].end : 0,
          this.tokenIndex < this.tokens.length ? this.tokens[this.tokenIndex].start : this.code.length
        );
        if (this.isFlowEnabled) {
          whitespaceAndComments = whitespaceAndComments.replace(/@flow/g, "");
        }
        return whitespaceAndComments;
      }
      replaceToken(newCode) {
        this.resultCode += this.previousWhitespaceAndComments();
        this.appendTokenPrefix();
        this.resultMappings[this.tokenIndex] = this.resultCode.length;
        this.resultCode += newCode;
        this.appendTokenSuffix();
        this.tokenIndex++;
      }
      replaceTokenTrimmingLeftWhitespace(newCode) {
        this.resultCode += this.previousWhitespaceAndComments().replace(/[^\r\n]/g, "");
        this.appendTokenPrefix();
        this.resultMappings[this.tokenIndex] = this.resultCode.length;
        this.resultCode += newCode;
        this.appendTokenSuffix();
        this.tokenIndex++;
      }
      removeInitialToken() {
        this.replaceToken("");
      }
      removeToken() {
        this.replaceTokenTrimmingLeftWhitespace("");
      }
      /**
       * Remove all code until the next }, accounting for balanced braces.
       */
      removeBalancedCode() {
        let braceDepth = 0;
        while (!this.isAtEnd()) {
          if (this.matches1(_types.TokenType.braceL)) {
            braceDepth++;
          } else if (this.matches1(_types.TokenType.braceR)) {
            if (braceDepth === 0) {
              return;
            }
            braceDepth--;
          }
          this.removeToken();
        }
      }
      copyExpectedToken(tokenType) {
        if (this.tokens[this.tokenIndex].type !== tokenType) {
          throw new Error(`Expected token ${tokenType}`);
        }
        this.copyToken();
      }
      copyToken() {
        this.resultCode += this.previousWhitespaceAndComments();
        this.appendTokenPrefix();
        this.resultMappings[this.tokenIndex] = this.resultCode.length;
        this.resultCode += this.code.slice(
          this.tokens[this.tokenIndex].start,
          this.tokens[this.tokenIndex].end
        );
        this.appendTokenSuffix();
        this.tokenIndex++;
      }
      copyTokenWithPrefix(prefix) {
        this.resultCode += this.previousWhitespaceAndComments();
        this.appendTokenPrefix();
        this.resultCode += prefix;
        this.resultMappings[this.tokenIndex] = this.resultCode.length;
        this.resultCode += this.code.slice(
          this.tokens[this.tokenIndex].start,
          this.tokens[this.tokenIndex].end
        );
        this.appendTokenSuffix();
        this.tokenIndex++;
      }
      appendTokenPrefix() {
        const token = this.currentToken();
        if (token.numNullishCoalesceStarts || token.isOptionalChainStart) {
          token.isAsyncOperation = _isAsyncOperation2.default.call(void 0, this);
        }
        if (this.disableESTransforms) {
          return;
        }
        if (token.numNullishCoalesceStarts) {
          for (let i = 0; i < token.numNullishCoalesceStarts; i++) {
            if (token.isAsyncOperation) {
              this.resultCode += "await ";
              this.resultCode += this.helperManager.getHelperName("asyncNullishCoalesce");
            } else {
              this.resultCode += this.helperManager.getHelperName("nullishCoalesce");
            }
            this.resultCode += "(";
          }
        }
        if (token.isOptionalChainStart) {
          if (token.isAsyncOperation) {
            this.resultCode += "await ";
          }
          if (this.tokenIndex > 0 && this.tokenAtRelativeIndex(-1).type === _types.TokenType._delete) {
            if (token.isAsyncOperation) {
              this.resultCode += this.helperManager.getHelperName("asyncOptionalChainDelete");
            } else {
              this.resultCode += this.helperManager.getHelperName("optionalChainDelete");
            }
          } else if (token.isAsyncOperation) {
            this.resultCode += this.helperManager.getHelperName("asyncOptionalChain");
          } else {
            this.resultCode += this.helperManager.getHelperName("optionalChain");
          }
          this.resultCode += "([";
        }
      }
      appendTokenSuffix() {
        const token = this.currentToken();
        if (token.isOptionalChainEnd && !this.disableESTransforms) {
          this.resultCode += "])";
        }
        if (token.numNullishCoalesceEnds && !this.disableESTransforms) {
          for (let i = 0; i < token.numNullishCoalesceEnds; i++) {
            this.resultCode += "))";
          }
        }
      }
      appendCode(code) {
        this.resultCode += code;
      }
      currentToken() {
        return this.tokens[this.tokenIndex];
      }
      currentTokenCode() {
        const token = this.currentToken();
        return this.code.slice(token.start, token.end);
      }
      tokenAtRelativeIndex(relativeIndex) {
        return this.tokens[this.tokenIndex + relativeIndex];
      }
      currentIndex() {
        return this.tokenIndex;
      }
      /**
       * Move to the next token. Only suitable in preprocessing steps. When
       * generating new code, you should use copyToken or removeToken.
       */
      nextToken() {
        if (this.tokenIndex === this.tokens.length) {
          throw new Error("Unexpectedly reached end of input.");
        }
        this.tokenIndex++;
      }
      previousToken() {
        this.tokenIndex--;
      }
      finish() {
        if (this.tokenIndex !== this.tokens.length) {
          throw new Error("Tried to finish processing tokens before reaching the end.");
        }
        this.resultCode += this.previousWhitespaceAndComments();
        return { code: this.resultCode, mappings: this.resultMappings };
      }
      isAtEnd() {
        return this.tokenIndex === this.tokens.length;
      }
    };
    exports.default = TokenProcessor;
  }
});

// node_modules/sucrase/dist/util/getClassInfo.js
var require_getClassInfo = __commonJS({
  "node_modules/sucrase/dist/util/getClassInfo.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    var _types = require_types();
    function getClassInfo(rootTransformer, tokens, nameManager, disableESTransforms) {
      const snapshot = tokens.snapshot();
      const headerInfo = processClassHeader(tokens);
      let constructorInitializerStatements = [];
      const instanceInitializerNames = [];
      const staticInitializerNames = [];
      let constructorInsertPos = null;
      const fields = [];
      const rangesToRemove = [];
      const classContextId = tokens.currentToken().contextId;
      if (classContextId == null) {
        throw new Error("Expected non-null class context ID on class open-brace.");
      }
      tokens.nextToken();
      while (!tokens.matchesContextIdAndLabel(_types.TokenType.braceR, classContextId)) {
        if (tokens.matchesContextual(_keywords.ContextualKeyword._constructor) && !tokens.currentToken().isType) {
          ({ constructorInitializerStatements, constructorInsertPos } = processConstructor(tokens));
        } else if (tokens.matches1(_types.TokenType.semi)) {
          if (!disableESTransforms) {
            rangesToRemove.push({ start: tokens.currentIndex(), end: tokens.currentIndex() + 1 });
          }
          tokens.nextToken();
        } else if (tokens.currentToken().isType) {
          tokens.nextToken();
        } else {
          const statementStartIndex = tokens.currentIndex();
          let isStatic = false;
          let isESPrivate = false;
          let isDeclareOrAbstract = false;
          while (isAccessModifier(tokens.currentToken())) {
            if (tokens.matches1(_types.TokenType._static)) {
              isStatic = true;
            }
            if (tokens.matches1(_types.TokenType.hash)) {
              isESPrivate = true;
            }
            if (tokens.matches1(_types.TokenType._declare) || tokens.matches1(_types.TokenType._abstract)) {
              isDeclareOrAbstract = true;
            }
            tokens.nextToken();
          }
          if (isStatic && tokens.matches1(_types.TokenType.braceL)) {
            skipToNextClassElement(tokens, classContextId);
            continue;
          }
          if (isESPrivate) {
            skipToNextClassElement(tokens, classContextId);
            continue;
          }
          if (tokens.matchesContextual(_keywords.ContextualKeyword._constructor) && !tokens.currentToken().isType) {
            ({ constructorInitializerStatements, constructorInsertPos } = processConstructor(tokens));
            continue;
          }
          const nameStartIndex = tokens.currentIndex();
          skipFieldName(tokens);
          if (tokens.matches1(_types.TokenType.lessThan) || tokens.matches1(_types.TokenType.parenL)) {
            skipToNextClassElement(tokens, classContextId);
            continue;
          }
          while (tokens.currentToken().isType) {
            tokens.nextToken();
          }
          if (tokens.matches1(_types.TokenType.eq)) {
            const equalsIndex = tokens.currentIndex();
            const valueEnd = tokens.currentToken().rhsEndIndex;
            if (valueEnd == null) {
              throw new Error("Expected rhsEndIndex on class field assignment.");
            }
            tokens.nextToken();
            while (tokens.currentIndex() < valueEnd) {
              rootTransformer.processToken();
            }
            let initializerName;
            if (isStatic) {
              initializerName = nameManager.claimFreeName("__initStatic");
              staticInitializerNames.push(initializerName);
            } else {
              initializerName = nameManager.claimFreeName("__init");
              instanceInitializerNames.push(initializerName);
            }
            fields.push({
              initializerName,
              equalsIndex,
              start: nameStartIndex,
              end: tokens.currentIndex()
            });
          } else if (!disableESTransforms || isDeclareOrAbstract) {
            rangesToRemove.push({ start: statementStartIndex, end: tokens.currentIndex() });
          }
        }
      }
      tokens.restoreToSnapshot(snapshot);
      if (disableESTransforms) {
        return {
          headerInfo,
          constructorInitializerStatements,
          instanceInitializerNames: [],
          staticInitializerNames: [],
          constructorInsertPos,
          fields: [],
          rangesToRemove
        };
      } else {
        return {
          headerInfo,
          constructorInitializerStatements,
          instanceInitializerNames,
          staticInitializerNames,
          constructorInsertPos,
          fields,
          rangesToRemove
        };
      }
    }
    exports.default = getClassInfo;
    function skipToNextClassElement(tokens, classContextId) {
      tokens.nextToken();
      while (tokens.currentToken().contextId !== classContextId) {
        tokens.nextToken();
      }
      while (isAccessModifier(tokens.tokenAtRelativeIndex(-1))) {
        tokens.previousToken();
      }
    }
    function processClassHeader(tokens) {
      const classToken = tokens.currentToken();
      const contextId = classToken.contextId;
      if (contextId == null) {
        throw new Error("Expected context ID on class token.");
      }
      const isExpression = classToken.isExpression;
      if (isExpression == null) {
        throw new Error("Expected isExpression on class token.");
      }
      let className = null;
      let hasSuperclass = false;
      tokens.nextToken();
      if (tokens.matches1(_types.TokenType.name)) {
        className = tokens.identifierName();
      }
      while (!tokens.matchesContextIdAndLabel(_types.TokenType.braceL, contextId)) {
        if (tokens.matches1(_types.TokenType._extends) && !tokens.currentToken().isType) {
          hasSuperclass = true;
        }
        tokens.nextToken();
      }
      return { isExpression, className, hasSuperclass };
    }
    function processConstructor(tokens) {
      const constructorInitializerStatements = [];
      tokens.nextToken();
      const constructorContextId = tokens.currentToken().contextId;
      if (constructorContextId == null) {
        throw new Error("Expected context ID on open-paren starting constructor params.");
      }
      while (!tokens.matchesContextIdAndLabel(_types.TokenType.parenR, constructorContextId)) {
        if (tokens.currentToken().contextId === constructorContextId) {
          tokens.nextToken();
          if (isAccessModifier(tokens.currentToken())) {
            tokens.nextToken();
            while (isAccessModifier(tokens.currentToken())) {
              tokens.nextToken();
            }
            const token = tokens.currentToken();
            if (token.type !== _types.TokenType.name) {
              throw new Error("Expected identifier after access modifiers in constructor arg.");
            }
            const name = tokens.identifierNameForToken(token);
            constructorInitializerStatements.push(`this.${name} = ${name}`);
          }
        } else {
          tokens.nextToken();
        }
      }
      tokens.nextToken();
      while (tokens.currentToken().isType) {
        tokens.nextToken();
      }
      let constructorInsertPos = tokens.currentIndex();
      let foundSuperCall = false;
      while (!tokens.matchesContextIdAndLabel(_types.TokenType.braceR, constructorContextId)) {
        if (!foundSuperCall && tokens.matches2(_types.TokenType._super, _types.TokenType.parenL)) {
          tokens.nextToken();
          const superCallContextId = tokens.currentToken().contextId;
          if (superCallContextId == null) {
            throw new Error("Expected a context ID on the super call");
          }
          while (!tokens.matchesContextIdAndLabel(_types.TokenType.parenR, superCallContextId)) {
            tokens.nextToken();
          }
          constructorInsertPos = tokens.currentIndex();
          foundSuperCall = true;
        }
        tokens.nextToken();
      }
      tokens.nextToken();
      return { constructorInitializerStatements, constructorInsertPos };
    }
    function isAccessModifier(token) {
      return [
        _types.TokenType._async,
        _types.TokenType._get,
        _types.TokenType._set,
        _types.TokenType.plus,
        _types.TokenType.minus,
        _types.TokenType._readonly,
        _types.TokenType._static,
        _types.TokenType._public,
        _types.TokenType._private,
        _types.TokenType._protected,
        _types.TokenType._override,
        _types.TokenType._abstract,
        _types.TokenType.star,
        _types.TokenType._declare,
        _types.TokenType.hash
      ].includes(token.type);
    }
    function skipFieldName(tokens) {
      if (tokens.matches1(_types.TokenType.bracketL)) {
        const startToken = tokens.currentToken();
        const classContextId = startToken.contextId;
        if (classContextId == null) {
          throw new Error("Expected class context ID on computed name open bracket.");
        }
        while (!tokens.matchesContextIdAndLabel(_types.TokenType.bracketR, classContextId)) {
          tokens.nextToken();
        }
        tokens.nextToken();
      } else {
        tokens.nextToken();
      }
    }
  }
});

// node_modules/sucrase/dist/util/elideImportEquals.js
var require_elideImportEquals = __commonJS({
  "node_modules/sucrase/dist/util/elideImportEquals.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _types = require_types();
    function elideImportEquals(tokens) {
      tokens.removeInitialToken();
      tokens.removeToken();
      tokens.removeToken();
      tokens.removeToken();
      if (tokens.matches1(_types.TokenType.parenL)) {
        tokens.removeToken();
        tokens.removeToken();
        tokens.removeToken();
      } else {
        while (tokens.matches1(_types.TokenType.dot)) {
          tokens.removeToken();
          tokens.removeToken();
        }
      }
    }
    exports.default = elideImportEquals;
  }
});

// node_modules/sucrase/dist/util/getDeclarationInfo.js
var require_getDeclarationInfo = __commonJS({
  "node_modules/sucrase/dist/util/getDeclarationInfo.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _tokenizer = require_tokenizer();
    var _types = require_types();
    var EMPTY_DECLARATION_INFO = {
      typeDeclarations: /* @__PURE__ */ new Set(),
      valueDeclarations: /* @__PURE__ */ new Set()
    };
    exports.EMPTY_DECLARATION_INFO = EMPTY_DECLARATION_INFO;
    function getDeclarationInfo(tokens) {
      const typeDeclarations = /* @__PURE__ */ new Set();
      const valueDeclarations = /* @__PURE__ */ new Set();
      for (let i = 0; i < tokens.tokens.length; i++) {
        const token = tokens.tokens[i];
        if (token.type === _types.TokenType.name && _tokenizer.isTopLevelDeclaration.call(void 0, token)) {
          if (token.isType) {
            typeDeclarations.add(tokens.identifierNameForToken(token));
          } else {
            valueDeclarations.add(tokens.identifierNameForToken(token));
          }
        }
      }
      return { typeDeclarations, valueDeclarations };
    }
    exports.default = getDeclarationInfo;
  }
});

// node_modules/sucrase/dist/util/isExportFrom.js
var require_isExportFrom = __commonJS({
  "node_modules/sucrase/dist/util/isExportFrom.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    var _types = require_types();
    function isExportFrom(tokens) {
      let closeBraceIndex = tokens.currentIndex();
      while (!tokens.matches1AtIndex(closeBraceIndex, _types.TokenType.braceR)) {
        closeBraceIndex++;
      }
      return tokens.matchesContextualAtIndex(closeBraceIndex + 1, _keywords.ContextualKeyword._from) && tokens.matches1AtIndex(closeBraceIndex + 2, _types.TokenType.string);
    }
    exports.default = isExportFrom;
  }
});

// node_modules/sucrase/dist/util/removeMaybeImportAttributes.js
var require_removeMaybeImportAttributes = __commonJS({
  "node_modules/sucrase/dist/util/removeMaybeImportAttributes.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _keywords = require_keywords();
    var _types = require_types();
    function removeMaybeImportAttributes(tokens) {
      if (tokens.matches2(_types.TokenType._with, _types.TokenType.braceL) || tokens.matches2(_types.TokenType.name, _types.TokenType.braceL) && tokens.matchesContextual(_keywords.ContextualKeyword._assert)) {
        tokens.removeToken();
        tokens.removeToken();
        tokens.removeBalancedCode();
        tokens.removeToken();
      }
    }
    exports.removeMaybeImportAttributes = removeMaybeImportAttributes;
  }
});

// node_modules/sucrase/dist/util/shouldElideDefaultExport.js
var require_shouldElideDefaultExport = __commonJS({
  "node_modules/sucrase/dist/util/shouldElideDefaultExport.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _types = require_types();
    function shouldElideDefaultExport(isTypeScriptTransformEnabled, keepUnusedImports, tokens, declarationInfo) {
      if (!isTypeScriptTransformEnabled || keepUnusedImports) {
        return false;
      }
      const exportToken = tokens.currentToken();
      if (exportToken.rhsEndIndex == null) {
        throw new Error("Expected non-null rhsEndIndex on export token.");
      }
      const numTokens = exportToken.rhsEndIndex - tokens.currentIndex();
      if (numTokens !== 3 && !(numTokens === 4 && tokens.matches1AtIndex(exportToken.rhsEndIndex - 1, _types.TokenType.semi))) {
        return false;
      }
      const identifierToken = tokens.tokenAtRelativeIndex(2);
      if (identifierToken.type !== _types.TokenType.name) {
        return false;
      }
      const exportedName = tokens.identifierNameForToken(identifierToken);
      return declarationInfo.typeDeclarations.has(exportedName) && !declarationInfo.valueDeclarations.has(exportedName);
    }
    exports.default = shouldElideDefaultExport;
  }
});

// node_modules/sucrase/dist/transformers/CJSImportTransformer.js
var require_CJSImportTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/CJSImportTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tokenizer = require_tokenizer();
    var _keywords = require_keywords();
    var _types = require_types();
    var _elideImportEquals = require_elideImportEquals();
    var _elideImportEquals2 = _interopRequireDefault(_elideImportEquals);
    var _getDeclarationInfo = require_getDeclarationInfo();
    var _getDeclarationInfo2 = _interopRequireDefault(_getDeclarationInfo);
    var _getImportExportSpecifierInfo = require_getImportExportSpecifierInfo();
    var _getImportExportSpecifierInfo2 = _interopRequireDefault(_getImportExportSpecifierInfo);
    var _isExportFrom = require_isExportFrom();
    var _isExportFrom2 = _interopRequireDefault(_isExportFrom);
    var _removeMaybeImportAttributes = require_removeMaybeImportAttributes();
    var _shouldElideDefaultExport = require_shouldElideDefaultExport();
    var _shouldElideDefaultExport2 = _interopRequireDefault(_shouldElideDefaultExport);
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var CJSImportTransformer = class _CJSImportTransformer extends _Transformer2.default {
      __init() {
        this.hadExport = false;
      }
      __init2() {
        this.hadNamedExport = false;
      }
      __init3() {
        this.hadDefaultExport = false;
      }
      constructor(rootTransformer, tokens, importProcessor, nameManager, helperManager, reactHotLoaderTransformer, enableLegacyBabel5ModuleInterop, enableLegacyTypeScriptModuleInterop, isTypeScriptTransformEnabled, isFlowTransformEnabled, preserveDynamicImport, keepUnusedImports) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.importProcessor = importProcessor;
        this.nameManager = nameManager;
        this.helperManager = helperManager;
        this.reactHotLoaderTransformer = reactHotLoaderTransformer;
        this.enableLegacyBabel5ModuleInterop = enableLegacyBabel5ModuleInterop;
        this.enableLegacyTypeScriptModuleInterop = enableLegacyTypeScriptModuleInterop;
        this.isTypeScriptTransformEnabled = isTypeScriptTransformEnabled;
        this.isFlowTransformEnabled = isFlowTransformEnabled;
        this.preserveDynamicImport = preserveDynamicImport;
        this.keepUnusedImports = keepUnusedImports;
        _CJSImportTransformer.prototype.__init.call(this);
        _CJSImportTransformer.prototype.__init2.call(this);
        _CJSImportTransformer.prototype.__init3.call(this);
        ;
        this.declarationInfo = isTypeScriptTransformEnabled ? _getDeclarationInfo2.default.call(void 0, tokens) : _getDeclarationInfo.EMPTY_DECLARATION_INFO;
      }
      getPrefixCode() {
        let prefix = "";
        if (this.hadExport) {
          prefix += 'Object.defineProperty(exports, "__esModule", {value: true});';
        }
        return prefix;
      }
      getSuffixCode() {
        if (this.enableLegacyBabel5ModuleInterop && this.hadDefaultExport && !this.hadNamedExport) {
          return "\nmodule.exports = exports.default;\n";
        }
        return "";
      }
      process() {
        if (this.tokens.matches3(_types.TokenType._import, _types.TokenType.name, _types.TokenType.eq)) {
          return this.processImportEquals();
        }
        if (this.tokens.matches1(_types.TokenType._import)) {
          this.processImport();
          return true;
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.eq)) {
          this.tokens.replaceToken("module.exports");
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._export) && !this.tokens.currentToken().isType) {
          this.hadExport = true;
          return this.processExport();
        }
        if (this.tokens.matches2(_types.TokenType.name, _types.TokenType.postIncDec)) {
          if (this.processPostIncDec()) {
            return true;
          }
        }
        if (this.tokens.matches1(_types.TokenType.name) || this.tokens.matches1(_types.TokenType.jsxName)) {
          return this.processIdentifier();
        }
        if (this.tokens.matches1(_types.TokenType.eq)) {
          return this.processAssignment();
        }
        if (this.tokens.matches1(_types.TokenType.assign)) {
          return this.processComplexAssignment();
        }
        if (this.tokens.matches1(_types.TokenType.preIncDec)) {
          return this.processPreIncDec();
        }
        return false;
      }
      processImportEquals() {
        const importName = this.tokens.identifierNameAtIndex(this.tokens.currentIndex() + 1);
        if (this.importProcessor.shouldAutomaticallyElideImportedName(importName)) {
          _elideImportEquals2.default.call(void 0, this.tokens);
        } else {
          this.tokens.replaceToken("const");
        }
        return true;
      }
      /**
       * Transform this:
       * import foo, {bar} from 'baz';
       * into
       * var _baz = require('baz'); var _baz2 = _interopRequireDefault(_baz);
       *
       * The import code was already generated in the import preprocessing step, so
       * we just need to look it up.
       */
      processImport() {
        if (this.tokens.matches2(_types.TokenType._import, _types.TokenType.parenL)) {
          if (this.preserveDynamicImport) {
            this.tokens.copyToken();
            return;
          }
          const requireWrapper = this.enableLegacyTypeScriptModuleInterop ? "" : `${this.helperManager.getHelperName("interopRequireWildcard")}(`;
          this.tokens.replaceToken(`Promise.resolve().then(() => ${requireWrapper}require`);
          const contextId = this.tokens.currentToken().contextId;
          if (contextId == null) {
            throw new Error("Expected context ID on dynamic import invocation.");
          }
          this.tokens.copyToken();
          while (!this.tokens.matchesContextIdAndLabel(_types.TokenType.parenR, contextId)) {
            this.rootTransformer.processToken();
          }
          this.tokens.replaceToken(requireWrapper ? ")))" : "))");
          return;
        }
        const shouldElideImport = this.removeImportAndDetectIfShouldElide();
        if (shouldElideImport) {
          this.tokens.removeToken();
        } else {
          const path = this.tokens.stringValue();
          this.tokens.replaceTokenTrimmingLeftWhitespace(this.importProcessor.claimImportCode(path));
          this.tokens.appendCode(this.importProcessor.claimImportCode(path));
        }
        _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
        if (this.tokens.matches1(_types.TokenType.semi)) {
          this.tokens.removeToken();
        }
      }
      /**
       * Erase this import (since any CJS output would be completely different), and
       * return true if this import is should be elided due to being a type-only
       * import. Such imports will not be emitted at all to avoid side effects.
       *
       * Import elision only happens with the TypeScript or Flow transforms enabled.
       *
       * TODO: This function has some awkward overlap with
       *  CJSImportProcessor.pruneTypeOnlyImports , and the two should be unified.
       *  That function handles TypeScript implicit import name elision, and removes
       *  an import if all typical imported names (without `type`) are removed due
       *  to being type-only imports. This function handles Flow import removal and
       *  properly distinguishes `import 'foo'` from `import {} from 'foo'` for TS
       *  purposes.
       *
       * The position should end at the import string.
       */
      removeImportAndDetectIfShouldElide() {
        this.tokens.removeInitialToken();
        if (this.tokens.matchesContextual(_keywords.ContextualKeyword._type) && !this.tokens.matches1AtIndex(this.tokens.currentIndex() + 1, _types.TokenType.comma) && !this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 1, _keywords.ContextualKeyword._from)) {
          this.removeRemainingImport();
          return true;
        }
        if (this.tokens.matches1(_types.TokenType.name) || this.tokens.matches1(_types.TokenType.star)) {
          this.removeRemainingImport();
          return false;
        }
        if (this.tokens.matches1(_types.TokenType.string)) {
          return false;
        }
        let foundNonTypeImport = false;
        let foundAnyNamedImport = false;
        while (!this.tokens.matches1(_types.TokenType.string)) {
          if (!foundNonTypeImport && this.tokens.matches1(_types.TokenType.braceL) || this.tokens.matches1(_types.TokenType.comma)) {
            this.tokens.removeToken();
            if (!this.tokens.matches1(_types.TokenType.braceR)) {
              foundAnyNamedImport = true;
            }
            if (this.tokens.matches2(_types.TokenType.name, _types.TokenType.comma) || this.tokens.matches2(_types.TokenType.name, _types.TokenType.braceR) || this.tokens.matches4(_types.TokenType.name, _types.TokenType.name, _types.TokenType.name, _types.TokenType.comma) || this.tokens.matches4(_types.TokenType.name, _types.TokenType.name, _types.TokenType.name, _types.TokenType.braceR)) {
              foundNonTypeImport = true;
            }
          }
          this.tokens.removeToken();
        }
        if (this.keepUnusedImports) {
          return false;
        }
        if (this.isTypeScriptTransformEnabled) {
          return !foundNonTypeImport;
        } else if (this.isFlowTransformEnabled) {
          return foundAnyNamedImport && !foundNonTypeImport;
        } else {
          return false;
        }
      }
      removeRemainingImport() {
        while (!this.tokens.matches1(_types.TokenType.string)) {
          this.tokens.removeToken();
        }
      }
      processIdentifier() {
        const token = this.tokens.currentToken();
        if (token.shadowsGlobal) {
          return false;
        }
        if (token.identifierRole === _tokenizer.IdentifierRole.ObjectShorthand) {
          return this.processObjectShorthand();
        }
        if (token.identifierRole !== _tokenizer.IdentifierRole.Access) {
          return false;
        }
        const replacement = this.importProcessor.getIdentifierReplacement(
          this.tokens.identifierNameForToken(token)
        );
        if (!replacement) {
          return false;
        }
        let possibleOpenParenIndex = this.tokens.currentIndex() + 1;
        while (possibleOpenParenIndex < this.tokens.tokens.length && this.tokens.tokens[possibleOpenParenIndex].type === _types.TokenType.parenR) {
          possibleOpenParenIndex++;
        }
        if (this.tokens.tokens[possibleOpenParenIndex].type === _types.TokenType.parenL) {
          if (this.tokens.tokenAtRelativeIndex(1).type === _types.TokenType.parenL && this.tokens.tokenAtRelativeIndex(-1).type !== _types.TokenType._new) {
            this.tokens.replaceToken(`${replacement}.call(void 0, `);
            this.tokens.removeToken();
            this.rootTransformer.processBalancedCode();
            this.tokens.copyExpectedToken(_types.TokenType.parenR);
          } else {
            this.tokens.replaceToken(`(0, ${replacement})`);
          }
        } else {
          this.tokens.replaceToken(replacement);
        }
        return true;
      }
      processObjectShorthand() {
        const identifier = this.tokens.identifierName();
        const replacement = this.importProcessor.getIdentifierReplacement(identifier);
        if (!replacement) {
          return false;
        }
        this.tokens.replaceToken(`${identifier}: ${replacement}`);
        return true;
      }
      processExport() {
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._enum) || this.tokens.matches3(_types.TokenType._export, _types.TokenType._const, _types.TokenType._enum)) {
          this.hadNamedExport = true;
          return false;
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._default)) {
          if (this.tokens.matches3(_types.TokenType._export, _types.TokenType._default, _types.TokenType._enum)) {
            this.hadDefaultExport = true;
            return false;
          }
          this.processExportDefault();
          return true;
        } else if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.braceL)) {
          this.processExportBindings();
          return true;
        } else if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.name) && this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 1, _keywords.ContextualKeyword._type)) {
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          if (this.tokens.matches1(_types.TokenType.braceL)) {
            while (!this.tokens.matches1(_types.TokenType.braceR)) {
              this.tokens.removeToken();
            }
            this.tokens.removeToken();
          } else {
            this.tokens.removeToken();
            if (this.tokens.matches1(_types.TokenType._as)) {
              this.tokens.removeToken();
              this.tokens.removeToken();
            }
          }
          if (this.tokens.matchesContextual(_keywords.ContextualKeyword._from) && this.tokens.matches1AtIndex(this.tokens.currentIndex() + 1, _types.TokenType.string)) {
            this.tokens.removeToken();
            this.tokens.removeToken();
            _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
          }
          return true;
        }
        this.hadNamedExport = true;
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._var) || this.tokens.matches2(_types.TokenType._export, _types.TokenType._let) || this.tokens.matches2(_types.TokenType._export, _types.TokenType._const)) {
          this.processExportVar();
          return true;
        } else if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._function) || // export async function
        this.tokens.matches3(_types.TokenType._export, _types.TokenType.name, _types.TokenType._function)) {
          this.processExportFunction();
          return true;
        } else if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._class) || this.tokens.matches3(_types.TokenType._export, _types.TokenType._abstract, _types.TokenType._class) || this.tokens.matches2(_types.TokenType._export, _types.TokenType.at)) {
          this.processExportClass();
          return true;
        } else if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.star)) {
          this.processExportStar();
          return true;
        } else {
          throw new Error("Unrecognized export syntax.");
        }
      }
      processAssignment() {
        const index = this.tokens.currentIndex();
        const identifierToken = this.tokens.tokens[index - 1];
        if (identifierToken.isType || identifierToken.type !== _types.TokenType.name) {
          return false;
        }
        if (identifierToken.shadowsGlobal) {
          return false;
        }
        if (index >= 2 && this.tokens.matches1AtIndex(index - 2, _types.TokenType.dot)) {
          return false;
        }
        if (index >= 2 && [_types.TokenType._var, _types.TokenType._let, _types.TokenType._const].includes(this.tokens.tokens[index - 2].type)) {
          return false;
        }
        const assignmentSnippet = this.importProcessor.resolveExportBinding(
          this.tokens.identifierNameForToken(identifierToken)
        );
        if (!assignmentSnippet) {
          return false;
        }
        this.tokens.copyToken();
        this.tokens.appendCode(` ${assignmentSnippet} =`);
        return true;
      }
      /**
       * Process something like `a += 3`, where `a` might be an exported value.
       */
      processComplexAssignment() {
        const index = this.tokens.currentIndex();
        const identifierToken = this.tokens.tokens[index - 1];
        if (identifierToken.type !== _types.TokenType.name) {
          return false;
        }
        if (identifierToken.shadowsGlobal) {
          return false;
        }
        if (index >= 2 && this.tokens.matches1AtIndex(index - 2, _types.TokenType.dot)) {
          return false;
        }
        const assignmentSnippet = this.importProcessor.resolveExportBinding(
          this.tokens.identifierNameForToken(identifierToken)
        );
        if (!assignmentSnippet) {
          return false;
        }
        this.tokens.appendCode(` = ${assignmentSnippet}`);
        this.tokens.copyToken();
        return true;
      }
      /**
       * Process something like `++a`, where `a` might be an exported value.
       */
      processPreIncDec() {
        const index = this.tokens.currentIndex();
        const identifierToken = this.tokens.tokens[index + 1];
        if (identifierToken.type !== _types.TokenType.name) {
          return false;
        }
        if (identifierToken.shadowsGlobal) {
          return false;
        }
        if (index + 2 < this.tokens.tokens.length && (this.tokens.matches1AtIndex(index + 2, _types.TokenType.dot) || this.tokens.matches1AtIndex(index + 2, _types.TokenType.bracketL) || this.tokens.matches1AtIndex(index + 2, _types.TokenType.parenL))) {
          return false;
        }
        const identifierName = this.tokens.identifierNameForToken(identifierToken);
        const assignmentSnippet = this.importProcessor.resolveExportBinding(identifierName);
        if (!assignmentSnippet) {
          return false;
        }
        this.tokens.appendCode(`${assignmentSnippet} = `);
        this.tokens.copyToken();
        return true;
      }
      /**
       * Process something like `a++`, where `a` might be an exported value.
       * This starts at the `a`, not at the `++`.
       */
      processPostIncDec() {
        const index = this.tokens.currentIndex();
        const identifierToken = this.tokens.tokens[index];
        const operatorToken = this.tokens.tokens[index + 1];
        if (identifierToken.type !== _types.TokenType.name) {
          return false;
        }
        if (identifierToken.shadowsGlobal) {
          return false;
        }
        if (index >= 1 && this.tokens.matches1AtIndex(index - 1, _types.TokenType.dot)) {
          return false;
        }
        const identifierName = this.tokens.identifierNameForToken(identifierToken);
        const assignmentSnippet = this.importProcessor.resolveExportBinding(identifierName);
        if (!assignmentSnippet) {
          return false;
        }
        const operatorCode = this.tokens.rawCodeForToken(operatorToken);
        const base = this.importProcessor.getIdentifierReplacement(identifierName) || identifierName;
        if (operatorCode === "++") {
          this.tokens.replaceToken(`(${base} = ${assignmentSnippet} = ${base} + 1, ${base} - 1)`);
        } else if (operatorCode === "--") {
          this.tokens.replaceToken(`(${base} = ${assignmentSnippet} = ${base} - 1, ${base} + 1)`);
        } else {
          throw new Error(`Unexpected operator: ${operatorCode}`);
        }
        this.tokens.removeToken();
        return true;
      }
      processExportDefault() {
        let exportedRuntimeValue = true;
        if (this.tokens.matches4(_types.TokenType._export, _types.TokenType._default, _types.TokenType._function, _types.TokenType.name) || // export default async function
        this.tokens.matches5(_types.TokenType._export, _types.TokenType._default, _types.TokenType.name, _types.TokenType._function, _types.TokenType.name) && this.tokens.matchesContextualAtIndex(
          this.tokens.currentIndex() + 2,
          _keywords.ContextualKeyword._async
        )) {
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          const name = this.processNamedFunction();
          this.tokens.appendCode(` exports.default = ${name};`);
        } else if (this.tokens.matches4(_types.TokenType._export, _types.TokenType._default, _types.TokenType._class, _types.TokenType.name) || this.tokens.matches5(_types.TokenType._export, _types.TokenType._default, _types.TokenType._abstract, _types.TokenType._class, _types.TokenType.name) || this.tokens.matches3(_types.TokenType._export, _types.TokenType._default, _types.TokenType.at)) {
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          this.copyDecorators();
          if (this.tokens.matches1(_types.TokenType._abstract)) {
            this.tokens.removeToken();
          }
          const name = this.rootTransformer.processNamedClass();
          this.tokens.appendCode(` exports.default = ${name};`);
        } else if (_shouldElideDefaultExport2.default.call(
          void 0,
          this.isTypeScriptTransformEnabled,
          this.keepUnusedImports,
          this.tokens,
          this.declarationInfo
        )) {
          exportedRuntimeValue = false;
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          this.tokens.removeToken();
        } else if (this.reactHotLoaderTransformer) {
          const defaultVarName = this.nameManager.claimFreeName("_default");
          this.tokens.replaceToken(`let ${defaultVarName}; exports.`);
          this.tokens.copyToken();
          this.tokens.appendCode(` = ${defaultVarName} =`);
          this.reactHotLoaderTransformer.setExtractedDefaultExportName(defaultVarName);
        } else {
          this.tokens.replaceToken("exports.");
          this.tokens.copyToken();
          this.tokens.appendCode(" =");
        }
        if (exportedRuntimeValue) {
          this.hadDefaultExport = true;
        }
      }
      copyDecorators() {
        while (this.tokens.matches1(_types.TokenType.at)) {
          this.tokens.copyToken();
          if (this.tokens.matches1(_types.TokenType.parenL)) {
            this.tokens.copyExpectedToken(_types.TokenType.parenL);
            this.rootTransformer.processBalancedCode();
            this.tokens.copyExpectedToken(_types.TokenType.parenR);
          } else {
            this.tokens.copyExpectedToken(_types.TokenType.name);
            while (this.tokens.matches1(_types.TokenType.dot)) {
              this.tokens.copyExpectedToken(_types.TokenType.dot);
              this.tokens.copyExpectedToken(_types.TokenType.name);
            }
            if (this.tokens.matches1(_types.TokenType.parenL)) {
              this.tokens.copyExpectedToken(_types.TokenType.parenL);
              this.rootTransformer.processBalancedCode();
              this.tokens.copyExpectedToken(_types.TokenType.parenR);
            }
          }
        }
      }
      /**
       * Transform a declaration like `export var`, `export let`, or `export const`.
       */
      processExportVar() {
        if (this.isSimpleExportVar()) {
          this.processSimpleExportVar();
        } else {
          this.processComplexExportVar();
        }
      }
      /**
       * Determine if the export is of the form:
       * export var/let/const [varName] = [expr];
       * In other words, determine if function name inference might apply.
       */
      isSimpleExportVar() {
        let tokenIndex = this.tokens.currentIndex();
        tokenIndex++;
        tokenIndex++;
        if (!this.tokens.matches1AtIndex(tokenIndex, _types.TokenType.name)) {
          return false;
        }
        tokenIndex++;
        while (tokenIndex < this.tokens.tokens.length && this.tokens.tokens[tokenIndex].isType) {
          tokenIndex++;
        }
        if (!this.tokens.matches1AtIndex(tokenIndex, _types.TokenType.eq)) {
          return false;
        }
        return true;
      }
      /**
       * Transform an `export var` declaration initializing a single variable.
       *
       * For example, this:
       * export const f = () => {};
       * becomes this:
       * const f = () => {}; exports.f = f;
       *
       * The variable is unused (e.g. exports.f has the true value of the export).
       * We need to produce an assignment of this form so that the function will
       * have an inferred name of "f", which wouldn't happen in the more general
       * case below.
       */
      processSimpleExportVar() {
        this.tokens.removeInitialToken();
        this.tokens.copyToken();
        const varName = this.tokens.identifierName();
        while (!this.tokens.matches1(_types.TokenType.eq)) {
          this.rootTransformer.processToken();
        }
        const endIndex = this.tokens.currentToken().rhsEndIndex;
        if (endIndex == null) {
          throw new Error("Expected = token with an end index.");
        }
        while (this.tokens.currentIndex() < endIndex) {
          this.rootTransformer.processToken();
        }
        this.tokens.appendCode(`; exports.${varName} = ${varName}`);
      }
      /**
       * Transform normal declaration exports, including handling destructuring.
       * For example, this:
       * export const {x: [a = 2, b], c} = d;
       * becomes this:
       * ({x: [exports.a = 2, exports.b], c: exports.c} = d;)
       */
      processComplexExportVar() {
        this.tokens.removeInitialToken();
        this.tokens.removeToken();
        const needsParens = this.tokens.matches1(_types.TokenType.braceL);
        if (needsParens) {
          this.tokens.appendCode("(");
        }
        let depth = 0;
        while (true) {
          if (this.tokens.matches1(_types.TokenType.braceL) || this.tokens.matches1(_types.TokenType.dollarBraceL) || this.tokens.matches1(_types.TokenType.bracketL)) {
            depth++;
            this.tokens.copyToken();
          } else if (this.tokens.matches1(_types.TokenType.braceR) || this.tokens.matches1(_types.TokenType.bracketR)) {
            depth--;
            this.tokens.copyToken();
          } else if (depth === 0 && !this.tokens.matches1(_types.TokenType.name) && !this.tokens.currentToken().isType) {
            break;
          } else if (this.tokens.matches1(_types.TokenType.eq)) {
            const endIndex = this.tokens.currentToken().rhsEndIndex;
            if (endIndex == null) {
              throw new Error("Expected = token with an end index.");
            }
            while (this.tokens.currentIndex() < endIndex) {
              this.rootTransformer.processToken();
            }
          } else {
            const token = this.tokens.currentToken();
            if (_tokenizer.isDeclaration.call(void 0, token)) {
              const name = this.tokens.identifierName();
              let replacement = this.importProcessor.getIdentifierReplacement(name);
              if (replacement === null) {
                throw new Error(`Expected a replacement for ${name} in \`export var\` syntax.`);
              }
              if (_tokenizer.isObjectShorthandDeclaration.call(void 0, token)) {
                replacement = `${name}: ${replacement}`;
              }
              this.tokens.replaceToken(replacement);
            } else {
              this.rootTransformer.processToken();
            }
          }
        }
        if (needsParens) {
          const endIndex = this.tokens.currentToken().rhsEndIndex;
          if (endIndex == null) {
            throw new Error("Expected = token with an end index.");
          }
          while (this.tokens.currentIndex() < endIndex) {
            this.rootTransformer.processToken();
          }
          this.tokens.appendCode(")");
        }
      }
      /**
       * Transform this:
       * export function foo() {}
       * into this:
       * function foo() {} exports.foo = foo;
       */
      processExportFunction() {
        this.tokens.replaceToken("");
        const name = this.processNamedFunction();
        this.tokens.appendCode(` exports.${name} = ${name};`);
      }
      /**
       * Skip past a function with a name and return that name.
       */
      processNamedFunction() {
        if (this.tokens.matches1(_types.TokenType._function)) {
          this.tokens.copyToken();
        } else if (this.tokens.matches2(_types.TokenType.name, _types.TokenType._function)) {
          if (!this.tokens.matchesContextual(_keywords.ContextualKeyword._async)) {
            throw new Error("Expected async keyword in function export.");
          }
          this.tokens.copyToken();
          this.tokens.copyToken();
        }
        if (this.tokens.matches1(_types.TokenType.star)) {
          this.tokens.copyToken();
        }
        if (!this.tokens.matches1(_types.TokenType.name)) {
          throw new Error("Expected identifier for exported function name.");
        }
        const name = this.tokens.identifierName();
        this.tokens.copyToken();
        if (this.tokens.currentToken().isType) {
          this.tokens.removeInitialToken();
          while (this.tokens.currentToken().isType) {
            this.tokens.removeToken();
          }
        }
        this.tokens.copyExpectedToken(_types.TokenType.parenL);
        this.rootTransformer.processBalancedCode();
        this.tokens.copyExpectedToken(_types.TokenType.parenR);
        this.rootTransformer.processPossibleTypeRange();
        this.tokens.copyExpectedToken(_types.TokenType.braceL);
        this.rootTransformer.processBalancedCode();
        this.tokens.copyExpectedToken(_types.TokenType.braceR);
        return name;
      }
      /**
       * Transform this:
       * export class A {}
       * into this:
       * class A {} exports.A = A;
       */
      processExportClass() {
        this.tokens.removeInitialToken();
        this.copyDecorators();
        if (this.tokens.matches1(_types.TokenType._abstract)) {
          this.tokens.removeToken();
        }
        const name = this.rootTransformer.processNamedClass();
        this.tokens.appendCode(` exports.${name} = ${name};`);
      }
      /**
       * Transform this:
       * export {a, b as c};
       * into this:
       * exports.a = a; exports.c = b;
       *
       * OR
       *
       * Transform this:
       * export {a, b as c} from './foo';
       * into the pre-generated Object.defineProperty code from the ImportProcessor.
       *
       * For the first case, if the TypeScript transform is enabled, we need to skip
       * exports that are only defined as types.
       */
      processExportBindings() {
        this.tokens.removeInitialToken();
        this.tokens.removeToken();
        const isReExport = _isExportFrom2.default.call(void 0, this.tokens);
        const exportStatements = [];
        while (true) {
          if (this.tokens.matches1(_types.TokenType.braceR)) {
            this.tokens.removeToken();
            break;
          }
          const specifierInfo = _getImportExportSpecifierInfo2.default.call(void 0, this.tokens);
          while (this.tokens.currentIndex() < specifierInfo.endIndex) {
            this.tokens.removeToken();
          }
          const shouldRemoveExport = specifierInfo.isType || !isReExport && this.shouldElideExportedIdentifier(specifierInfo.leftName);
          if (!shouldRemoveExport) {
            const exportedName = specifierInfo.rightName;
            if (exportedName === "default") {
              this.hadDefaultExport = true;
            } else {
              this.hadNamedExport = true;
            }
            const localName = specifierInfo.leftName;
            const newLocalName = this.importProcessor.getIdentifierReplacement(localName);
            exportStatements.push(`exports.${exportedName} = ${newLocalName || localName};`);
          }
          if (this.tokens.matches1(_types.TokenType.braceR)) {
            this.tokens.removeToken();
            break;
          }
          if (this.tokens.matches2(_types.TokenType.comma, _types.TokenType.braceR)) {
            this.tokens.removeToken();
            this.tokens.removeToken();
            break;
          } else if (this.tokens.matches1(_types.TokenType.comma)) {
            this.tokens.removeToken();
          } else {
            throw new Error(`Unexpected token: ${JSON.stringify(this.tokens.currentToken())}`);
          }
        }
        if (this.tokens.matchesContextual(_keywords.ContextualKeyword._from)) {
          this.tokens.removeToken();
          const path = this.tokens.stringValue();
          this.tokens.replaceTokenTrimmingLeftWhitespace(this.importProcessor.claimImportCode(path));
          _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
        } else {
          this.tokens.appendCode(exportStatements.join(" "));
        }
        if (this.tokens.matches1(_types.TokenType.semi)) {
          this.tokens.removeToken();
        }
      }
      processExportStar() {
        this.tokens.removeInitialToken();
        while (!this.tokens.matches1(_types.TokenType.string)) {
          this.tokens.removeToken();
        }
        const path = this.tokens.stringValue();
        this.tokens.replaceTokenTrimmingLeftWhitespace(this.importProcessor.claimImportCode(path));
        _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
        if (this.tokens.matches1(_types.TokenType.semi)) {
          this.tokens.removeToken();
        }
      }
      shouldElideExportedIdentifier(name) {
        return this.isTypeScriptTransformEnabled && !this.keepUnusedImports && !this.declarationInfo.valueDeclarations.has(name);
      }
    };
    exports.default = CJSImportTransformer;
  }
});

// node_modules/sucrase/dist/transformers/ESMImportTransformer.js
var require_ESMImportTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/ESMImportTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _keywords = require_keywords();
    var _types = require_types();
    var _elideImportEquals = require_elideImportEquals();
    var _elideImportEquals2 = _interopRequireDefault(_elideImportEquals);
    var _getDeclarationInfo = require_getDeclarationInfo();
    var _getDeclarationInfo2 = _interopRequireDefault(_getDeclarationInfo);
    var _getImportExportSpecifierInfo = require_getImportExportSpecifierInfo();
    var _getImportExportSpecifierInfo2 = _interopRequireDefault(_getImportExportSpecifierInfo);
    var _getNonTypeIdentifiers = require_getNonTypeIdentifiers();
    var _isExportFrom = require_isExportFrom();
    var _isExportFrom2 = _interopRequireDefault(_isExportFrom);
    var _removeMaybeImportAttributes = require_removeMaybeImportAttributes();
    var _shouldElideDefaultExport = require_shouldElideDefaultExport();
    var _shouldElideDefaultExport2 = _interopRequireDefault(_shouldElideDefaultExport);
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var ESMImportTransformer = class extends _Transformer2.default {
      constructor(tokens, nameManager, helperManager, reactHotLoaderTransformer, isTypeScriptTransformEnabled, isFlowTransformEnabled, keepUnusedImports, options) {
        super();
        this.tokens = tokens;
        this.nameManager = nameManager;
        this.helperManager = helperManager;
        this.reactHotLoaderTransformer = reactHotLoaderTransformer;
        this.isTypeScriptTransformEnabled = isTypeScriptTransformEnabled;
        this.isFlowTransformEnabled = isFlowTransformEnabled;
        this.keepUnusedImports = keepUnusedImports;
        ;
        this.nonTypeIdentifiers = isTypeScriptTransformEnabled && !keepUnusedImports ? _getNonTypeIdentifiers.getNonTypeIdentifiers.call(void 0, tokens, options) : /* @__PURE__ */ new Set();
        this.declarationInfo = isTypeScriptTransformEnabled && !keepUnusedImports ? _getDeclarationInfo2.default.call(void 0, tokens) : _getDeclarationInfo.EMPTY_DECLARATION_INFO;
        this.injectCreateRequireForImportRequire = Boolean(options.injectCreateRequireForImportRequire);
      }
      process() {
        if (this.tokens.matches3(_types.TokenType._import, _types.TokenType.name, _types.TokenType.eq)) {
          return this.processImportEquals();
        }
        if (this.tokens.matches4(_types.TokenType._import, _types.TokenType.name, _types.TokenType.name, _types.TokenType.eq) && this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 1, _keywords.ContextualKeyword._type)) {
          this.tokens.removeInitialToken();
          for (let i = 0; i < 7; i++) {
            this.tokens.removeToken();
          }
          return true;
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.eq)) {
          this.tokens.replaceToken("module.exports");
          return true;
        }
        if (this.tokens.matches5(_types.TokenType._export, _types.TokenType._import, _types.TokenType.name, _types.TokenType.name, _types.TokenType.eq) && this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 2, _keywords.ContextualKeyword._type)) {
          this.tokens.removeInitialToken();
          for (let i = 0; i < 8; i++) {
            this.tokens.removeToken();
          }
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._import)) {
          return this.processImport();
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._default)) {
          return this.processExportDefault();
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.braceL)) {
          return this.processNamedExports();
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType.name) && this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 1, _keywords.ContextualKeyword._type)) {
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          if (this.tokens.matches1(_types.TokenType.braceL)) {
            while (!this.tokens.matches1(_types.TokenType.braceR)) {
              this.tokens.removeToken();
            }
            this.tokens.removeToken();
          } else {
            this.tokens.removeToken();
            if (this.tokens.matches1(_types.TokenType._as)) {
              this.tokens.removeToken();
              this.tokens.removeToken();
            }
          }
          if (this.tokens.matchesContextual(_keywords.ContextualKeyword._from) && this.tokens.matches1AtIndex(this.tokens.currentIndex() + 1, _types.TokenType.string)) {
            this.tokens.removeToken();
            this.tokens.removeToken();
            _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
          }
          return true;
        }
        return false;
      }
      processImportEquals() {
        const importName = this.tokens.identifierNameAtIndex(this.tokens.currentIndex() + 1);
        if (this.shouldAutomaticallyElideImportedName(importName)) {
          _elideImportEquals2.default.call(void 0, this.tokens);
        } else if (this.injectCreateRequireForImportRequire) {
          this.tokens.replaceToken("const");
          this.tokens.copyToken();
          this.tokens.copyToken();
          this.tokens.replaceToken(this.helperManager.getHelperName("require"));
        } else {
          this.tokens.replaceToken("const");
        }
        return true;
      }
      processImport() {
        if (this.tokens.matches2(_types.TokenType._import, _types.TokenType.parenL)) {
          return false;
        }
        const snapshot = this.tokens.snapshot();
        const allImportsRemoved = this.removeImportTypeBindings();
        if (allImportsRemoved) {
          this.tokens.restoreToSnapshot(snapshot);
          while (!this.tokens.matches1(_types.TokenType.string)) {
            this.tokens.removeToken();
          }
          this.tokens.removeToken();
          _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
          if (this.tokens.matches1(_types.TokenType.semi)) {
            this.tokens.removeToken();
          }
        }
        return true;
      }
      /**
       * Remove type bindings from this import, leaving the rest of the import intact.
       *
       * Return true if this import was ONLY types, and thus is eligible for removal. This will bail out
       * of the replacement operation, so we can return early here.
       */
      removeImportTypeBindings() {
        this.tokens.copyExpectedToken(_types.TokenType._import);
        if (this.tokens.matchesContextual(_keywords.ContextualKeyword._type) && !this.tokens.matches1AtIndex(this.tokens.currentIndex() + 1, _types.TokenType.comma) && !this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 1, _keywords.ContextualKeyword._from)) {
          return true;
        }
        if (this.tokens.matches1(_types.TokenType.string)) {
          this.tokens.copyToken();
          return false;
        }
        if (this.tokens.matchesContextual(_keywords.ContextualKeyword._module) && this.tokens.matchesContextualAtIndex(this.tokens.currentIndex() + 2, _keywords.ContextualKeyword._from)) {
          this.tokens.copyToken();
        }
        let foundNonTypeImport = false;
        let foundAnyNamedImport = false;
        let needsComma = false;
        if (this.tokens.matches1(_types.TokenType.name)) {
          if (this.shouldAutomaticallyElideImportedName(this.tokens.identifierName())) {
            this.tokens.removeToken();
            if (this.tokens.matches1(_types.TokenType.comma)) {
              this.tokens.removeToken();
            }
          } else {
            foundNonTypeImport = true;
            this.tokens.copyToken();
            if (this.tokens.matches1(_types.TokenType.comma)) {
              needsComma = true;
              this.tokens.removeToken();
            }
          }
        }
        if (this.tokens.matches1(_types.TokenType.star)) {
          if (this.shouldAutomaticallyElideImportedName(this.tokens.identifierNameAtRelativeIndex(2))) {
            this.tokens.removeToken();
            this.tokens.removeToken();
            this.tokens.removeToken();
          } else {
            if (needsComma) {
              this.tokens.appendCode(",");
            }
            foundNonTypeImport = true;
            this.tokens.copyExpectedToken(_types.TokenType.star);
            this.tokens.copyExpectedToken(_types.TokenType.name);
            this.tokens.copyExpectedToken(_types.TokenType.name);
          }
        } else if (this.tokens.matches1(_types.TokenType.braceL)) {
          if (needsComma) {
            this.tokens.appendCode(",");
          }
          this.tokens.copyToken();
          while (!this.tokens.matches1(_types.TokenType.braceR)) {
            foundAnyNamedImport = true;
            const specifierInfo = _getImportExportSpecifierInfo2.default.call(void 0, this.tokens);
            if (specifierInfo.isType || this.shouldAutomaticallyElideImportedName(specifierInfo.rightName)) {
              while (this.tokens.currentIndex() < specifierInfo.endIndex) {
                this.tokens.removeToken();
              }
              if (this.tokens.matches1(_types.TokenType.comma)) {
                this.tokens.removeToken();
              }
            } else {
              foundNonTypeImport = true;
              while (this.tokens.currentIndex() < specifierInfo.endIndex) {
                this.tokens.copyToken();
              }
              if (this.tokens.matches1(_types.TokenType.comma)) {
                this.tokens.copyToken();
              }
            }
          }
          this.tokens.copyExpectedToken(_types.TokenType.braceR);
        }
        if (this.keepUnusedImports) {
          return false;
        }
        if (this.isTypeScriptTransformEnabled) {
          return !foundNonTypeImport;
        } else if (this.isFlowTransformEnabled) {
          return foundAnyNamedImport && !foundNonTypeImport;
        } else {
          return false;
        }
      }
      shouldAutomaticallyElideImportedName(name) {
        return this.isTypeScriptTransformEnabled && !this.keepUnusedImports && !this.nonTypeIdentifiers.has(name);
      }
      processExportDefault() {
        if (_shouldElideDefaultExport2.default.call(
          void 0,
          this.isTypeScriptTransformEnabled,
          this.keepUnusedImports,
          this.tokens,
          this.declarationInfo
        )) {
          this.tokens.removeInitialToken();
          this.tokens.removeToken();
          this.tokens.removeToken();
          return true;
        }
        const alreadyHasName = this.tokens.matches4(_types.TokenType._export, _types.TokenType._default, _types.TokenType._function, _types.TokenType.name) || // export default async function
        this.tokens.matches5(_types.TokenType._export, _types.TokenType._default, _types.TokenType.name, _types.TokenType._function, _types.TokenType.name) && this.tokens.matchesContextualAtIndex(
          this.tokens.currentIndex() + 2,
          _keywords.ContextualKeyword._async
        ) || this.tokens.matches4(_types.TokenType._export, _types.TokenType._default, _types.TokenType._class, _types.TokenType.name) || this.tokens.matches5(_types.TokenType._export, _types.TokenType._default, _types.TokenType._abstract, _types.TokenType._class, _types.TokenType.name);
        if (!alreadyHasName && this.reactHotLoaderTransformer) {
          const defaultVarName = this.nameManager.claimFreeName("_default");
          this.tokens.replaceToken(`let ${defaultVarName}; export`);
          this.tokens.copyToken();
          this.tokens.appendCode(` ${defaultVarName} =`);
          this.reactHotLoaderTransformer.setExtractedDefaultExportName(defaultVarName);
          return true;
        }
        return false;
      }
      /**
       * Handle a statement with one of these forms:
       * export {a, type b};
       * export {c, type d} from 'foo';
       *
       * In both cases, any explicit type exports should be removed. In the first
       * case, we also need to handle implicit export elision for names declared as
       * types. In the second case, we must NOT do implicit named export elision,
       * but we must remove the runtime import if all exports are type exports.
       */
      processNamedExports() {
        if (!this.isTypeScriptTransformEnabled) {
          return false;
        }
        this.tokens.copyExpectedToken(_types.TokenType._export);
        this.tokens.copyExpectedToken(_types.TokenType.braceL);
        const isReExport = _isExportFrom2.default.call(void 0, this.tokens);
        let foundNonTypeExport = false;
        while (!this.tokens.matches1(_types.TokenType.braceR)) {
          const specifierInfo = _getImportExportSpecifierInfo2.default.call(void 0, this.tokens);
          if (specifierInfo.isType || !isReExport && this.shouldElideExportedName(specifierInfo.leftName)) {
            while (this.tokens.currentIndex() < specifierInfo.endIndex) {
              this.tokens.removeToken();
            }
            if (this.tokens.matches1(_types.TokenType.comma)) {
              this.tokens.removeToken();
            }
          } else {
            foundNonTypeExport = true;
            while (this.tokens.currentIndex() < specifierInfo.endIndex) {
              this.tokens.copyToken();
            }
            if (this.tokens.matches1(_types.TokenType.comma)) {
              this.tokens.copyToken();
            }
          }
        }
        this.tokens.copyExpectedToken(_types.TokenType.braceR);
        if (!this.keepUnusedImports && isReExport && !foundNonTypeExport) {
          this.tokens.removeToken();
          this.tokens.removeToken();
          _removeMaybeImportAttributes.removeMaybeImportAttributes.call(void 0, this.tokens);
        }
        return true;
      }
      /**
       * ESM elides all imports with the rule that we only elide if we see that it's
       * a type and never see it as a value. This is in contrast to CJS, which
       * elides imports that are completely unknown.
       */
      shouldElideExportedName(name) {
        return this.isTypeScriptTransformEnabled && !this.keepUnusedImports && this.declarationInfo.typeDeclarations.has(name) && !this.declarationInfo.valueDeclarations.has(name);
      }
    };
    exports.default = ESMImportTransformer;
  }
});

// node_modules/sucrase/dist/transformers/FlowTransformer.js
var require_FlowTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/FlowTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _keywords = require_keywords();
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var FlowTransformer = class extends _Transformer2.default {
      constructor(rootTransformer, tokens, isImportsTransformEnabled) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.isImportsTransformEnabled = isImportsTransformEnabled;
        ;
      }
      process() {
        if (this.rootTransformer.processPossibleArrowParamEnd() || this.rootTransformer.processPossibleAsyncArrowWithTypeParams() || this.rootTransformer.processPossibleTypeRange()) {
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._enum)) {
          this.processEnum();
          return true;
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._enum)) {
          this.processNamedExportEnum();
          return true;
        }
        if (this.tokens.matches3(_types.TokenType._export, _types.TokenType._default, _types.TokenType._enum)) {
          this.processDefaultExportEnum();
          return true;
        }
        return false;
      }
      /**
       * Handle a declaration like:
       * export enum E ...
       *
       * With this imports transform, this becomes:
       * const E = [[enum]]; exports.E = E;
       *
       * otherwise, it becomes:
       * export const E = [[enum]];
       */
      processNamedExportEnum() {
        if (this.isImportsTransformEnabled) {
          this.tokens.removeInitialToken();
          const enumName = this.tokens.identifierNameAtRelativeIndex(1);
          this.processEnum();
          this.tokens.appendCode(` exports.${enumName} = ${enumName};`);
        } else {
          this.tokens.copyToken();
          this.processEnum();
        }
      }
      /**
       * Handle a declaration like:
       * export default enum E
       *
       * With the imports transform, this becomes:
       * const E = [[enum]]; exports.default = E;
       *
       * otherwise, it becomes:
       * const E = [[enum]]; export default E;
       */
      processDefaultExportEnum() {
        this.tokens.removeInitialToken();
        this.tokens.removeToken();
        const enumName = this.tokens.identifierNameAtRelativeIndex(1);
        this.processEnum();
        if (this.isImportsTransformEnabled) {
          this.tokens.appendCode(` exports.default = ${enumName};`);
        } else {
          this.tokens.appendCode(` export default ${enumName};`);
        }
      }
      /**
       * Transpile flow enums to invoke the "flow-enums-runtime" library.
       *
       * Currently, the transpiled code always uses `require("flow-enums-runtime")`,
       * but if future flexibility is needed, we could expose a config option for
       * this string (similar to configurable JSX). Even when targeting ESM, the
       * default behavior of babel-plugin-transform-flow-enums is to use require
       * rather than injecting an import.
       *
       * Flow enums are quite a bit simpler than TS enums and have some convenient
       * constraints:
       * - Element initializers must be either always present or always absent. That
       *   means that we can use fixed lookahead on the first element (if any) and
       *   assume that all elements are like that.
       * - The right-hand side of an element initializer must be a literal value,
       *   not a complex expression and not referencing other elements. That means
       *   we can simply copy a single token.
       *
       * Enums can be broken up into three basic cases:
       *
       * Mirrored enums:
       * enum E {A, B}
       *   ->
       * const E = require("flow-enums-runtime").Mirrored(["A", "B"]);
       *
       * Initializer enums:
       * enum E {A = 1, B = 2}
       *   ->
       * const E = require("flow-enums-runtime")({A: 1, B: 2});
       *
       * Symbol enums:
       * enum E of symbol {A, B}
       *   ->
       * const E = require("flow-enums-runtime")({A: Symbol("A"), B: Symbol("B")});
       *
       * We can statically detect which of the three cases this is by looking at the
       * "of" declaration (if any) and seeing if the first element has an initializer.
       * Since the other transform details are so similar between the three cases, we
       * use a single implementation and vary the transform within processEnumElement
       * based on case.
       */
      processEnum() {
        this.tokens.replaceToken("const");
        this.tokens.copyExpectedToken(_types.TokenType.name);
        let isSymbolEnum = false;
        if (this.tokens.matchesContextual(_keywords.ContextualKeyword._of)) {
          this.tokens.removeToken();
          isSymbolEnum = this.tokens.matchesContextual(_keywords.ContextualKeyword._symbol);
          this.tokens.removeToken();
        }
        const hasInitializers = this.tokens.matches3(_types.TokenType.braceL, _types.TokenType.name, _types.TokenType.eq);
        this.tokens.appendCode(' = require("flow-enums-runtime")');
        const isMirrored = !isSymbolEnum && !hasInitializers;
        this.tokens.replaceTokenTrimmingLeftWhitespace(isMirrored ? ".Mirrored([" : "({");
        while (!this.tokens.matches1(_types.TokenType.braceR)) {
          if (this.tokens.matches1(_types.TokenType.ellipsis)) {
            this.tokens.removeToken();
            break;
          }
          this.processEnumElement(isSymbolEnum, hasInitializers);
          if (this.tokens.matches1(_types.TokenType.comma)) {
            this.tokens.copyToken();
          }
        }
        this.tokens.replaceToken(isMirrored ? "]);" : "});");
      }
      /**
       * Process an individual enum element, producing either an array element or an
       * object element based on what type of enum this is.
       */
      processEnumElement(isSymbolEnum, hasInitializers) {
        if (isSymbolEnum) {
          const elementName = this.tokens.identifierName();
          this.tokens.copyToken();
          this.tokens.appendCode(`: Symbol("${elementName}")`);
        } else if (hasInitializers) {
          this.tokens.copyToken();
          this.tokens.replaceTokenTrimmingLeftWhitespace(":");
          this.tokens.copyToken();
        } else {
          this.tokens.replaceToken(`"${this.tokens.identifierName()}"`);
        }
      }
    };
    exports.default = FlowTransformer;
  }
});

// node_modules/sucrase/dist/transformers/JestHoistTransformer.js
var require_JestHoistTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/JestHoistTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    function _optionalChain(ops) {
      let lastAccessLHS = void 0;
      let value = ops[0];
      let i = 1;
      while (i < ops.length) {
        const op = ops[i];
        const fn = ops[i + 1];
        i += 2;
        if ((op === "optionalAccess" || op === "optionalCall") && value == null) {
          return void 0;
        }
        if (op === "access" || op === "optionalAccess") {
          lastAccessLHS = value;
          value = fn(value);
        } else if (op === "call" || op === "optionalCall") {
          value = fn((...args) => value.call(lastAccessLHS, ...args));
          lastAccessLHS = void 0;
        }
      }
      return value;
    }
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var JEST_GLOBAL_NAME = "jest";
    var HOISTED_METHODS = ["mock", "unmock", "enableAutomock", "disableAutomock"];
    var JestHoistTransformer = class _JestHoistTransformer extends _Transformer2.default {
      __init() {
        this.hoistedFunctionNames = [];
      }
      constructor(rootTransformer, tokens, nameManager, importProcessor) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.nameManager = nameManager;
        this.importProcessor = importProcessor;
        _JestHoistTransformer.prototype.__init.call(this);
        ;
      }
      process() {
        if (this.tokens.currentToken().scopeDepth === 0 && this.tokens.matches4(_types.TokenType.name, _types.TokenType.dot, _types.TokenType.name, _types.TokenType.parenL) && this.tokens.identifierName() === JEST_GLOBAL_NAME) {
          if (_optionalChain([this, "access", (_) => _.importProcessor, "optionalAccess", (_2) => _2.getGlobalNames, "call", (_3) => _3(), "optionalAccess", (_4) => _4.has, "call", (_5) => _5(JEST_GLOBAL_NAME)])) {
            return false;
          }
          return this.extractHoistedCalls();
        }
        return false;
      }
      getHoistedCode() {
        if (this.hoistedFunctionNames.length > 0) {
          return this.hoistedFunctionNames.map((name) => `${name}();`).join("");
        }
        return "";
      }
      /**
       * Extracts any methods calls on the jest-object that should be hoisted.
       *
       * According to the jest docs, https://jestjs.io/docs/en/jest-object#jestmockmodulename-factory-options,
       * mock, unmock, enableAutomock, disableAutomock, are the methods that should be hoisted.
       *
       * We do not apply the same checks of the arguments as babel-plugin-jest-hoist does.
       */
      extractHoistedCalls() {
        this.tokens.removeToken();
        let followsNonHoistedJestCall = false;
        while (this.tokens.matches3(_types.TokenType.dot, _types.TokenType.name, _types.TokenType.parenL)) {
          const methodName = this.tokens.identifierNameAtIndex(this.tokens.currentIndex() + 1);
          const shouldHoist = HOISTED_METHODS.includes(methodName);
          if (shouldHoist) {
            const hoistedFunctionName = this.nameManager.claimFreeName("__jestHoist");
            this.hoistedFunctionNames.push(hoistedFunctionName);
            this.tokens.replaceToken(`function ${hoistedFunctionName}(){${JEST_GLOBAL_NAME}.`);
            this.tokens.copyToken();
            this.tokens.copyToken();
            this.rootTransformer.processBalancedCode();
            this.tokens.copyExpectedToken(_types.TokenType.parenR);
            this.tokens.appendCode(";}");
            followsNonHoistedJestCall = false;
          } else {
            if (followsNonHoistedJestCall) {
              this.tokens.copyToken();
            } else {
              this.tokens.replaceToken(`${JEST_GLOBAL_NAME}.`);
            }
            this.tokens.copyToken();
            this.tokens.copyToken();
            this.rootTransformer.processBalancedCode();
            this.tokens.copyExpectedToken(_types.TokenType.parenR);
            followsNonHoistedJestCall = true;
          }
        }
        return true;
      }
    };
    exports.default = JestHoistTransformer;
  }
});

// node_modules/sucrase/dist/transformers/NumericSeparatorTransformer.js
var require_NumericSeparatorTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/NumericSeparatorTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var NumericSeparatorTransformer = class extends _Transformer2.default {
      constructor(tokens) {
        super();
        this.tokens = tokens;
        ;
      }
      process() {
        if (this.tokens.matches1(_types.TokenType.num)) {
          const code = this.tokens.currentTokenCode();
          if (code.includes("_")) {
            this.tokens.replaceToken(code.replace(/_/g, ""));
            return true;
          }
        }
        return false;
      }
    };
    exports.default = NumericSeparatorTransformer;
  }
});

// node_modules/sucrase/dist/transformers/OptionalCatchBindingTransformer.js
var require_OptionalCatchBindingTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/OptionalCatchBindingTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var OptionalCatchBindingTransformer = class extends _Transformer2.default {
      constructor(tokens, nameManager) {
        super();
        this.tokens = tokens;
        this.nameManager = nameManager;
        ;
      }
      process() {
        if (this.tokens.matches2(_types.TokenType._catch, _types.TokenType.braceL)) {
          this.tokens.copyToken();
          this.tokens.appendCode(` (${this.nameManager.claimFreeName("e")})`);
          return true;
        }
        return false;
      }
    };
    exports.default = OptionalCatchBindingTransformer;
  }
});

// node_modules/sucrase/dist/transformers/OptionalChainingNullishTransformer.js
var require_OptionalChainingNullishTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/OptionalChainingNullishTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var OptionalChainingNullishTransformer = class extends _Transformer2.default {
      constructor(tokens, nameManager) {
        super();
        this.tokens = tokens;
        this.nameManager = nameManager;
        ;
      }
      process() {
        if (this.tokens.matches1(_types.TokenType.nullishCoalescing)) {
          const token2 = this.tokens.currentToken();
          if (this.tokens.tokens[token2.nullishStartIndex].isAsyncOperation) {
            this.tokens.replaceTokenTrimmingLeftWhitespace(", async () => (");
          } else {
            this.tokens.replaceTokenTrimmingLeftWhitespace(", () => (");
          }
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._delete)) {
          const nextToken = this.tokens.tokenAtRelativeIndex(1);
          if (nextToken.isOptionalChainStart) {
            this.tokens.removeInitialToken();
            return true;
          }
        }
        const token = this.tokens.currentToken();
        const chainStart = token.subscriptStartIndex;
        if (chainStart != null && this.tokens.tokens[chainStart].isOptionalChainStart && // Super subscripts can't be optional (since super is never null/undefined), and the syntax
        // relies on the subscript being intact, so leave this token alone.
        this.tokens.tokenAtRelativeIndex(-1).type !== _types.TokenType._super) {
          const param = this.nameManager.claimFreeName("_");
          let arrowStartSnippet;
          if (chainStart > 0 && this.tokens.matches1AtIndex(chainStart - 1, _types.TokenType._delete) && this.isLastSubscriptInChain()) {
            arrowStartSnippet = `${param} => delete ${param}`;
          } else {
            arrowStartSnippet = `${param} => ${param}`;
          }
          if (this.tokens.tokens[chainStart].isAsyncOperation) {
            arrowStartSnippet = `async ${arrowStartSnippet}`;
          }
          if (this.tokens.matches2(_types.TokenType.questionDot, _types.TokenType.parenL) || this.tokens.matches2(_types.TokenType.questionDot, _types.TokenType.lessThan)) {
            if (this.justSkippedSuper()) {
              this.tokens.appendCode(".bind(this)");
            }
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'optionalCall', ${arrowStartSnippet}`);
          } else if (this.tokens.matches2(_types.TokenType.questionDot, _types.TokenType.bracketL)) {
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'optionalAccess', ${arrowStartSnippet}`);
          } else if (this.tokens.matches1(_types.TokenType.questionDot)) {
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'optionalAccess', ${arrowStartSnippet}.`);
          } else if (this.tokens.matches1(_types.TokenType.dot)) {
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'access', ${arrowStartSnippet}.`);
          } else if (this.tokens.matches1(_types.TokenType.bracketL)) {
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'access', ${arrowStartSnippet}[`);
          } else if (this.tokens.matches1(_types.TokenType.parenL)) {
            if (this.justSkippedSuper()) {
              this.tokens.appendCode(".bind(this)");
            }
            this.tokens.replaceTokenTrimmingLeftWhitespace(`, 'call', ${arrowStartSnippet}(`);
          } else {
            throw new Error("Unexpected subscript operator in optional chain.");
          }
          return true;
        }
        return false;
      }
      /**
       * Determine if the current token is the last of its chain, so that we know whether it's eligible
       * to have a delete op inserted.
       *
       * We can do this by walking forward until we determine one way or another. Each
       * isOptionalChainStart token must be paired with exactly one isOptionalChainEnd token after it in
       * a nesting way, so we can track depth and walk to the end of the chain (the point where the
       * depth goes negative) and see if any other subscript token is after us in the chain.
       */
      isLastSubscriptInChain() {
        let depth = 0;
        for (let i = this.tokens.currentIndex() + 1; ; i++) {
          if (i >= this.tokens.tokens.length) {
            throw new Error("Reached the end of the code while finding the end of the access chain.");
          }
          if (this.tokens.tokens[i].isOptionalChainStart) {
            depth++;
          } else if (this.tokens.tokens[i].isOptionalChainEnd) {
            depth--;
          }
          if (depth < 0) {
            return true;
          }
          if (depth === 0 && this.tokens.tokens[i].subscriptStartIndex != null) {
            return false;
          }
        }
      }
      /**
       * Determine if we are the open-paren in an expression like super.a()?.b.
       *
       * We can do this by walking backward to find the previous subscript. If that subscript was
       * preceded by a super, then we must be the subscript after it, so if this is a call expression,
       * we'll need to attach the right context.
       */
      justSkippedSuper() {
        let depth = 0;
        let index = this.tokens.currentIndex() - 1;
        while (true) {
          if (index < 0) {
            throw new Error(
              "Reached the start of the code while finding the start of the access chain."
            );
          }
          if (this.tokens.tokens[index].isOptionalChainStart) {
            depth--;
          } else if (this.tokens.tokens[index].isOptionalChainEnd) {
            depth++;
          }
          if (depth < 0) {
            return false;
          }
          if (depth === 0 && this.tokens.tokens[index].subscriptStartIndex != null) {
            return this.tokens.tokens[index - 1].type === _types.TokenType._super;
          }
          index--;
        }
      }
    };
    exports.default = OptionalChainingNullishTransformer;
  }
});

// node_modules/sucrase/dist/transformers/ReactDisplayNameTransformer.js
var require_ReactDisplayNameTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/ReactDisplayNameTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tokenizer = require_tokenizer();
    var _types = require_types();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var ReactDisplayNameTransformer = class extends _Transformer2.default {
      constructor(rootTransformer, tokens, importProcessor, options) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.importProcessor = importProcessor;
        this.options = options;
        ;
      }
      process() {
        const startIndex = this.tokens.currentIndex();
        if (this.tokens.identifierName() === "createReactClass") {
          const newName = this.importProcessor && this.importProcessor.getIdentifierReplacement("createReactClass");
          if (newName) {
            this.tokens.replaceToken(`(0, ${newName})`);
          } else {
            this.tokens.copyToken();
          }
          this.tryProcessCreateClassCall(startIndex);
          return true;
        }
        if (this.tokens.matches3(_types.TokenType.name, _types.TokenType.dot, _types.TokenType.name) && this.tokens.identifierName() === "React" && this.tokens.identifierNameAtIndex(this.tokens.currentIndex() + 2) === "createClass") {
          const newName = this.importProcessor ? this.importProcessor.getIdentifierReplacement("React") || "React" : "React";
          if (newName) {
            this.tokens.replaceToken(newName);
            this.tokens.copyToken();
            this.tokens.copyToken();
          } else {
            this.tokens.copyToken();
            this.tokens.copyToken();
            this.tokens.copyToken();
          }
          this.tryProcessCreateClassCall(startIndex);
          return true;
        }
        return false;
      }
      /**
       * This is called with the token position at the open-paren.
       */
      tryProcessCreateClassCall(startIndex) {
        const displayName = this.findDisplayName(startIndex);
        if (!displayName) {
          return;
        }
        if (this.classNeedsDisplayName()) {
          this.tokens.copyExpectedToken(_types.TokenType.parenL);
          this.tokens.copyExpectedToken(_types.TokenType.braceL);
          this.tokens.appendCode(`displayName: '${displayName}',`);
          this.rootTransformer.processBalancedCode();
          this.tokens.copyExpectedToken(_types.TokenType.braceR);
          this.tokens.copyExpectedToken(_types.TokenType.parenR);
        }
      }
      findDisplayName(startIndex) {
        if (startIndex < 2) {
          return null;
        }
        if (this.tokens.matches2AtIndex(startIndex - 2, _types.TokenType.name, _types.TokenType.eq)) {
          return this.tokens.identifierNameAtIndex(startIndex - 2);
        }
        if (startIndex >= 2 && this.tokens.tokens[startIndex - 2].identifierRole === _tokenizer.IdentifierRole.ObjectKey) {
          return this.tokens.identifierNameAtIndex(startIndex - 2);
        }
        if (this.tokens.matches2AtIndex(startIndex - 2, _types.TokenType._export, _types.TokenType._default)) {
          return this.getDisplayNameFromFilename();
        }
        return null;
      }
      getDisplayNameFromFilename() {
        const filePath = this.options.filePath || "unknown";
        const pathSegments = filePath.split("/");
        const filename = pathSegments[pathSegments.length - 1];
        const dotIndex = filename.lastIndexOf(".");
        const baseFilename = dotIndex === -1 ? filename : filename.slice(0, dotIndex);
        if (baseFilename === "index" && pathSegments[pathSegments.length - 2]) {
          return pathSegments[pathSegments.length - 2];
        } else {
          return baseFilename;
        }
      }
      /**
       * We only want to add a display name when this is a function call containing
       * one argument, which is an object literal without `displayName` as an
       * existing key.
       */
      classNeedsDisplayName() {
        let index = this.tokens.currentIndex();
        if (!this.tokens.matches2(_types.TokenType.parenL, _types.TokenType.braceL)) {
          return false;
        }
        const objectStartIndex = index + 1;
        const objectContextId = this.tokens.tokens[objectStartIndex].contextId;
        if (objectContextId == null) {
          throw new Error("Expected non-null context ID on object open-brace.");
        }
        for (; index < this.tokens.tokens.length; index++) {
          const token = this.tokens.tokens[index];
          if (token.type === _types.TokenType.braceR && token.contextId === objectContextId) {
            index++;
            break;
          }
          if (this.tokens.identifierNameAtIndex(index) === "displayName" && this.tokens.tokens[index].identifierRole === _tokenizer.IdentifierRole.ObjectKey && token.contextId === objectContextId) {
            return false;
          }
        }
        if (index === this.tokens.tokens.length) {
          throw new Error("Unexpected end of input when processing React class.");
        }
        return this.tokens.matches1AtIndex(index, _types.TokenType.parenR) || this.tokens.matches2AtIndex(index, _types.TokenType.comma, _types.TokenType.parenR);
      }
    };
    exports.default = ReactDisplayNameTransformer;
  }
});

// node_modules/sucrase/dist/transformers/ReactHotLoaderTransformer.js
var require_ReactHotLoaderTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/ReactHotLoaderTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _tokenizer = require_tokenizer();
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var ReactHotLoaderTransformer = class _ReactHotLoaderTransformer extends _Transformer2.default {
      __init() {
        this.extractedDefaultExportName = null;
      }
      constructor(tokens, filePath) {
        super();
        this.tokens = tokens;
        this.filePath = filePath;
        _ReactHotLoaderTransformer.prototype.__init.call(this);
        ;
      }
      setExtractedDefaultExportName(extractedDefaultExportName) {
        this.extractedDefaultExportName = extractedDefaultExportName;
      }
      getPrefixCode() {
        return `
      (function () {
        var enterModule = require('react-hot-loader').enterModule;
        enterModule && enterModule(module);
      })();`.replace(/\s+/g, " ").trim();
      }
      getSuffixCode() {
        const topLevelNames = /* @__PURE__ */ new Set();
        for (const token of this.tokens.tokens) {
          if (!token.isType && _tokenizer.isTopLevelDeclaration.call(void 0, token) && token.identifierRole !== _tokenizer.IdentifierRole.ImportDeclaration) {
            topLevelNames.add(this.tokens.identifierNameForToken(token));
          }
        }
        const namesToRegister = Array.from(topLevelNames).map((name) => ({
          variableName: name,
          uniqueLocalName: name
        }));
        if (this.extractedDefaultExportName) {
          namesToRegister.push({
            variableName: this.extractedDefaultExportName,
            uniqueLocalName: "default"
          });
        }
        return `
;(function () {
  var reactHotLoader = require('react-hot-loader').default;
  var leaveModule = require('react-hot-loader').leaveModule;
  if (!reactHotLoader) {
    return;
  }
${namesToRegister.map(
          ({ variableName, uniqueLocalName }) => `  reactHotLoader.register(${variableName}, "${uniqueLocalName}", ${JSON.stringify(
            this.filePath || ""
          )});`
        ).join("\n")}
  leaveModule(module);
})();`;
      }
      process() {
        return false;
      }
    };
    exports.default = ReactHotLoaderTransformer;
  }
});

// node_modules/sucrase/dist/util/isIdentifier.js
var require_isIdentifier = __commonJS({
  "node_modules/sucrase/dist/util/isIdentifier.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var _identifier = require_identifier();
    var RESERVED_WORDS = /* @__PURE__ */ new Set([
      // Reserved keywords as of ECMAScript 2015
      "break",
      "case",
      "catch",
      "class",
      "const",
      "continue",
      "debugger",
      "default",
      "delete",
      "do",
      "else",
      "export",
      "extends",
      "finally",
      "for",
      "function",
      "if",
      "import",
      "in",
      "instanceof",
      "new",
      "return",
      "super",
      "switch",
      "this",
      "throw",
      "try",
      "typeof",
      "var",
      "void",
      "while",
      "with",
      "yield",
      // Future reserved keywords
      "enum",
      "implements",
      "interface",
      "let",
      "package",
      "private",
      "protected",
      "public",
      "static",
      "await",
      // Literals that cannot be used as identifiers
      "false",
      "null",
      "true"
    ]);
    function isIdentifier(name) {
      if (name.length === 0) {
        return false;
      }
      if (!_identifier.IS_IDENTIFIER_START[name.charCodeAt(0)]) {
        return false;
      }
      for (let i = 1; i < name.length; i++) {
        if (!_identifier.IS_IDENTIFIER_CHAR[name.charCodeAt(i)]) {
          return false;
        }
      }
      return !RESERVED_WORDS.has(name);
    }
    exports.default = isIdentifier;
  }
});

// node_modules/sucrase/dist/transformers/TypeScriptTransformer.js
var require_TypeScriptTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/TypeScriptTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _isIdentifier = require_isIdentifier();
    var _isIdentifier2 = _interopRequireDefault(_isIdentifier);
    var _Transformer = require_Transformer();
    var _Transformer2 = _interopRequireDefault(_Transformer);
    var TypeScriptTransformer = class extends _Transformer2.default {
      constructor(rootTransformer, tokens, isImportsTransformEnabled) {
        super();
        this.rootTransformer = rootTransformer;
        this.tokens = tokens;
        this.isImportsTransformEnabled = isImportsTransformEnabled;
        ;
      }
      process() {
        if (this.rootTransformer.processPossibleArrowParamEnd() || this.rootTransformer.processPossibleAsyncArrowWithTypeParams() || this.rootTransformer.processPossibleTypeRange()) {
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._public) || this.tokens.matches1(_types.TokenType._protected) || this.tokens.matches1(_types.TokenType._private) || this.tokens.matches1(_types.TokenType._abstract) || this.tokens.matches1(_types.TokenType._readonly) || this.tokens.matches1(_types.TokenType._override) || this.tokens.matches1(_types.TokenType.nonNullAssertion)) {
          this.tokens.removeInitialToken();
          return true;
        }
        if (this.tokens.matches1(_types.TokenType._enum) || this.tokens.matches2(_types.TokenType._const, _types.TokenType._enum)) {
          this.processEnum();
          return true;
        }
        if (this.tokens.matches2(_types.TokenType._export, _types.TokenType._enum) || this.tokens.matches3(_types.TokenType._export, _types.TokenType._const, _types.TokenType._enum)) {
          this.processEnum(true);
          return true;
        }
        return false;
      }
      processEnum(isExport = false) {
        this.tokens.removeInitialToken();
        while (this.tokens.matches1(_types.TokenType._const) || this.tokens.matches1(_types.TokenType._enum)) {
          this.tokens.removeToken();
        }
        const enumName = this.tokens.identifierName();
        this.tokens.removeToken();
        if (isExport && !this.isImportsTransformEnabled) {
          this.tokens.appendCode("export ");
        }
        this.tokens.appendCode(`var ${enumName}; (function (${enumName})`);
        this.tokens.copyExpectedToken(_types.TokenType.braceL);
        this.processEnumBody(enumName);
        this.tokens.copyExpectedToken(_types.TokenType.braceR);
        if (isExport && this.isImportsTransformEnabled) {
          this.tokens.appendCode(`)(${enumName} || (exports.${enumName} = ${enumName} = {}));`);
        } else {
          this.tokens.appendCode(`)(${enumName} || (${enumName} = {}));`);
        }
      }
      /**
       * Transform an enum into equivalent JS. This has complexity in a few places:
       * - TS allows string enums, numeric enums, and a mix of the two styles within an enum.
       * - Enum keys are allowed to be referenced in later enum values.
       * - Enum keys are allowed to be strings.
       * - When enum values are omitted, they should follow an auto-increment behavior.
       */
      processEnumBody(enumName) {
        let previousValueCode = null;
        while (true) {
          if (this.tokens.matches1(_types.TokenType.braceR)) {
            break;
          }
          const { nameStringCode, variableName } = this.extractEnumKeyInfo(this.tokens.currentToken());
          this.tokens.removeInitialToken();
          if (this.tokens.matches3(_types.TokenType.eq, _types.TokenType.string, _types.TokenType.comma) || this.tokens.matches3(_types.TokenType.eq, _types.TokenType.string, _types.TokenType.braceR)) {
            this.processStringLiteralEnumMember(enumName, nameStringCode, variableName);
          } else if (this.tokens.matches1(_types.TokenType.eq)) {
            this.processExplicitValueEnumMember(enumName, nameStringCode, variableName);
          } else {
            this.processImplicitValueEnumMember(
              enumName,
              nameStringCode,
              variableName,
              previousValueCode
            );
          }
          if (this.tokens.matches1(_types.TokenType.comma)) {
            this.tokens.removeToken();
          }
          if (variableName != null) {
            previousValueCode = variableName;
          } else {
            previousValueCode = `${enumName}[${nameStringCode}]`;
          }
        }
      }
      /**
       * Detect name information about this enum key, which will be used to determine which code to emit
       * and whether we should declare a variable as part of this declaration.
       *
       * Some cases to keep in mind:
       * - Enum keys can be implicitly referenced later, e.g. `X = 1, Y = X`. In Sucrase, we implement
       *   this by declaring a variable `X` so that later expressions can use it.
       * - In addition to the usual identifier key syntax, enum keys are allowed to be string literals,
       *   e.g. `"hello world" = 3,`. Template literal syntax is NOT allowed.
       * - Even if the enum key is defined as a string literal, it may still be referenced by identifier
       *   later, e.g. `"X" = 1, Y = X`. That means that we need to detect whether or not a string
       *   literal is identifier-like and emit a variable if so, even if the declaration did not use an
       *   identifier.
       * - Reserved keywords like `break` are valid enum keys, but are not valid to be referenced later
       *   and would be a syntax error if we emitted a variable, so we need to skip the variable
       *   declaration in those cases.
       *
       * The variableName return value captures these nuances: if non-null, we can and must emit a
       * variable declaration, and if null, we can't and shouldn't.
       */
      extractEnumKeyInfo(nameToken) {
        if (nameToken.type === _types.TokenType.name) {
          const name = this.tokens.identifierNameForToken(nameToken);
          return {
            nameStringCode: `"${name}"`,
            variableName: _isIdentifier2.default.call(void 0, name) ? name : null
          };
        } else if (nameToken.type === _types.TokenType.string) {
          const name = this.tokens.stringValueForToken(nameToken);
          return {
            nameStringCode: this.tokens.code.slice(nameToken.start, nameToken.end),
            variableName: _isIdentifier2.default.call(void 0, name) ? name : null
          };
        } else {
          throw new Error("Expected name or string at beginning of enum element.");
        }
      }
      /**
       * Handle an enum member where the RHS is just a string literal (not omitted, not a number, and
       * not a complex expression). This is the typical form for TS string enums, and in this case, we
       * do *not* create a reverse mapping.
       *
       * This is called after deleting the key token, when the token processor is at the equals sign.
       *
       * Example 1:
       * someKey = "some value"
       * ->
       * const someKey = "some value"; MyEnum["someKey"] = someKey;
       *
       * Example 2:
       * "some key" = "some value"
       * ->
       * MyEnum["some key"] = "some value";
       */
      processStringLiteralEnumMember(enumName, nameStringCode, variableName) {
        if (variableName != null) {
          this.tokens.appendCode(`const ${variableName}`);
          this.tokens.copyToken();
          this.tokens.copyToken();
          this.tokens.appendCode(`; ${enumName}[${nameStringCode}] = ${variableName};`);
        } else {
          this.tokens.appendCode(`${enumName}[${nameStringCode}]`);
          this.tokens.copyToken();
          this.tokens.copyToken();
          this.tokens.appendCode(";");
        }
      }
      /**
       * Handle an enum member initialized with an expression on the right-hand side (other than a
       * string literal). In these cases, we should transform the expression and emit code that sets up
       * a reverse mapping.
       *
       * The TypeScript implementation of this operation distinguishes between expressions that can be
       * "constant folded" at compile time (i.e. consist of number literals and simple math operations
       * on those numbers) and ones that are dynamic. For constant expressions, it emits the resolved
       * numeric value, and auto-incrementing is only allowed in that case. Evaluating expressions at
       * compile time would add significant complexity to Sucrase, so Sucrase instead leaves the
       * expression as-is, and will later emit something like `MyEnum["previousKey"] + 1` to implement
       * auto-incrementing.
       *
       * This is called after deleting the key token, when the token processor is at the equals sign.
       *
       * Example 1:
       * someKey = 1 + 1
       * ->
       * const someKey = 1 + 1; MyEnum[MyEnum["someKey"] = someKey] = "someKey";
       *
       * Example 2:
       * "some key" = 1 + 1
       * ->
       * MyEnum[MyEnum["some key"] = 1 + 1] = "some key";
       */
      processExplicitValueEnumMember(enumName, nameStringCode, variableName) {
        const rhsEndIndex = this.tokens.currentToken().rhsEndIndex;
        if (rhsEndIndex == null) {
          throw new Error("Expected rhsEndIndex on enum assign.");
        }
        if (variableName != null) {
          this.tokens.appendCode(`const ${variableName}`);
          this.tokens.copyToken();
          while (this.tokens.currentIndex() < rhsEndIndex) {
            this.rootTransformer.processToken();
          }
          this.tokens.appendCode(
            `; ${enumName}[${enumName}[${nameStringCode}] = ${variableName}] = ${nameStringCode};`
          );
        } else {
          this.tokens.appendCode(`${enumName}[${enumName}[${nameStringCode}]`);
          this.tokens.copyToken();
          while (this.tokens.currentIndex() < rhsEndIndex) {
            this.rootTransformer.processToken();
          }
          this.tokens.appendCode(`] = ${nameStringCode};`);
        }
      }
      /**
       * Handle an enum member with no right-hand side expression. In this case, the value is the
       * previous value plus 1, or 0 if there was no previous value. We should also always emit a
       * reverse mapping.
       *
       * Example 1:
       * someKey2
       * ->
       * const someKey2 = someKey1 + 1; MyEnum[MyEnum["someKey2"] = someKey2] = "someKey2";
       *
       * Example 2:
       * "some key 2"
       * ->
       * MyEnum[MyEnum["some key 2"] = someKey1 + 1] = "some key 2";
       */
      processImplicitValueEnumMember(enumName, nameStringCode, variableName, previousValueCode) {
        let valueCode = previousValueCode != null ? `${previousValueCode} + 1` : "0";
        if (variableName != null) {
          this.tokens.appendCode(`const ${variableName} = ${valueCode}; `);
          valueCode = variableName;
        }
        this.tokens.appendCode(
          `${enumName}[${enumName}[${nameStringCode}] = ${valueCode}] = ${nameStringCode};`
        );
      }
    };
    exports.default = TypeScriptTransformer;
  }
});

// node_modules/sucrase/dist/transformers/RootTransformer.js
var require_RootTransformer = __commonJS({
  "node_modules/sucrase/dist/transformers/RootTransformer.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _keywords = require_keywords();
    var _types = require_types();
    var _getClassInfo = require_getClassInfo();
    var _getClassInfo2 = _interopRequireDefault(_getClassInfo);
    var _CJSImportTransformer = require_CJSImportTransformer();
    var _CJSImportTransformer2 = _interopRequireDefault(_CJSImportTransformer);
    var _ESMImportTransformer = require_ESMImportTransformer();
    var _ESMImportTransformer2 = _interopRequireDefault(_ESMImportTransformer);
    var _FlowTransformer = require_FlowTransformer();
    var _FlowTransformer2 = _interopRequireDefault(_FlowTransformer);
    var _JestHoistTransformer = require_JestHoistTransformer();
    var _JestHoistTransformer2 = _interopRequireDefault(_JestHoistTransformer);
    var _JSXTransformer = require_JSXTransformer();
    var _JSXTransformer2 = _interopRequireDefault(_JSXTransformer);
    var _NumericSeparatorTransformer = require_NumericSeparatorTransformer();
    var _NumericSeparatorTransformer2 = _interopRequireDefault(_NumericSeparatorTransformer);
    var _OptionalCatchBindingTransformer = require_OptionalCatchBindingTransformer();
    var _OptionalCatchBindingTransformer2 = _interopRequireDefault(_OptionalCatchBindingTransformer);
    var _OptionalChainingNullishTransformer = require_OptionalChainingNullishTransformer();
    var _OptionalChainingNullishTransformer2 = _interopRequireDefault(_OptionalChainingNullishTransformer);
    var _ReactDisplayNameTransformer = require_ReactDisplayNameTransformer();
    var _ReactDisplayNameTransformer2 = _interopRequireDefault(_ReactDisplayNameTransformer);
    var _ReactHotLoaderTransformer = require_ReactHotLoaderTransformer();
    var _ReactHotLoaderTransformer2 = _interopRequireDefault(_ReactHotLoaderTransformer);
    var _TypeScriptTransformer = require_TypeScriptTransformer();
    var _TypeScriptTransformer2 = _interopRequireDefault(_TypeScriptTransformer);
    var RootTransformer = class _RootTransformer {
      __init() {
        this.transformers = [];
      }
      __init2() {
        this.generatedVariables = [];
      }
      constructor(sucraseContext, transforms, enableLegacyBabel5ModuleInterop, options) {
        ;
        _RootTransformer.prototype.__init.call(this);
        _RootTransformer.prototype.__init2.call(this);
        this.nameManager = sucraseContext.nameManager;
        this.helperManager = sucraseContext.helperManager;
        const { tokenProcessor, importProcessor } = sucraseContext;
        this.tokens = tokenProcessor;
        this.isImportsTransformEnabled = transforms.includes("imports");
        this.isReactHotLoaderTransformEnabled = transforms.includes("react-hot-loader");
        this.disableESTransforms = Boolean(options.disableESTransforms);
        if (!options.disableESTransforms) {
          this.transformers.push(
            new (0, _OptionalChainingNullishTransformer2.default)(tokenProcessor, this.nameManager)
          );
          this.transformers.push(new (0, _NumericSeparatorTransformer2.default)(tokenProcessor));
          this.transformers.push(new (0, _OptionalCatchBindingTransformer2.default)(tokenProcessor, this.nameManager));
        }
        if (transforms.includes("jsx")) {
          if (options.jsxRuntime !== "preserve") {
            this.transformers.push(
              new (0, _JSXTransformer2.default)(this, tokenProcessor, importProcessor, this.nameManager, options)
            );
          }
          this.transformers.push(
            new (0, _ReactDisplayNameTransformer2.default)(this, tokenProcessor, importProcessor, options)
          );
        }
        let reactHotLoaderTransformer = null;
        if (transforms.includes("react-hot-loader")) {
          if (!options.filePath) {
            throw new Error("filePath is required when using the react-hot-loader transform.");
          }
          reactHotLoaderTransformer = new (0, _ReactHotLoaderTransformer2.default)(tokenProcessor, options.filePath);
          this.transformers.push(reactHotLoaderTransformer);
        }
        if (transforms.includes("imports")) {
          if (importProcessor === null) {
            throw new Error("Expected non-null importProcessor with imports transform enabled.");
          }
          this.transformers.push(
            new (0, _CJSImportTransformer2.default)(
              this,
              tokenProcessor,
              importProcessor,
              this.nameManager,
              this.helperManager,
              reactHotLoaderTransformer,
              enableLegacyBabel5ModuleInterop,
              Boolean(options.enableLegacyTypeScriptModuleInterop),
              transforms.includes("typescript"),
              transforms.includes("flow"),
              Boolean(options.preserveDynamicImport),
              Boolean(options.keepUnusedImports)
            )
          );
        } else {
          this.transformers.push(
            new (0, _ESMImportTransformer2.default)(
              tokenProcessor,
              this.nameManager,
              this.helperManager,
              reactHotLoaderTransformer,
              transforms.includes("typescript"),
              transforms.includes("flow"),
              Boolean(options.keepUnusedImports),
              options
            )
          );
        }
        if (transforms.includes("flow")) {
          this.transformers.push(
            new (0, _FlowTransformer2.default)(this, tokenProcessor, transforms.includes("imports"))
          );
        }
        if (transforms.includes("typescript")) {
          this.transformers.push(
            new (0, _TypeScriptTransformer2.default)(this, tokenProcessor, transforms.includes("imports"))
          );
        }
        if (transforms.includes("jest")) {
          this.transformers.push(
            new (0, _JestHoistTransformer2.default)(this, tokenProcessor, this.nameManager, importProcessor)
          );
        }
      }
      transform() {
        this.tokens.reset();
        this.processBalancedCode();
        const shouldAddUseStrict = this.isImportsTransformEnabled;
        let prefix = shouldAddUseStrict ? '"use strict";' : "";
        for (const transformer of this.transformers) {
          prefix += transformer.getPrefixCode();
        }
        prefix += this.helperManager.emitHelpers();
        prefix += this.generatedVariables.map((v) => ` var ${v};`).join("");
        for (const transformer of this.transformers) {
          prefix += transformer.getHoistedCode();
        }
        let suffix = "";
        for (const transformer of this.transformers) {
          suffix += transformer.getSuffixCode();
        }
        const result = this.tokens.finish();
        let { code } = result;
        if (code.startsWith("#!")) {
          let newlineIndex = code.indexOf("\n");
          if (newlineIndex === -1) {
            newlineIndex = code.length;
            code += "\n";
          }
          return {
            code: code.slice(0, newlineIndex + 1) + prefix + code.slice(newlineIndex + 1) + suffix,
            // The hashbang line has no tokens, so shifting the tokens to account
            // for prefix can happen normally.
            mappings: this.shiftMappings(result.mappings, prefix.length)
          };
        } else {
          return {
            code: prefix + code + suffix,
            mappings: this.shiftMappings(result.mappings, prefix.length)
          };
        }
      }
      processBalancedCode() {
        let braceDepth = 0;
        let parenDepth = 0;
        while (!this.tokens.isAtEnd()) {
          if (this.tokens.matches1(_types.TokenType.braceL) || this.tokens.matches1(_types.TokenType.dollarBraceL)) {
            braceDepth++;
          } else if (this.tokens.matches1(_types.TokenType.braceR)) {
            if (braceDepth === 0) {
              return;
            }
            braceDepth--;
          }
          if (this.tokens.matches1(_types.TokenType.parenL)) {
            parenDepth++;
          } else if (this.tokens.matches1(_types.TokenType.parenR)) {
            if (parenDepth === 0) {
              return;
            }
            parenDepth--;
          }
          this.processToken();
        }
      }
      processToken() {
        if (this.tokens.matches1(_types.TokenType._class)) {
          this.processClass();
          return;
        }
        for (const transformer of this.transformers) {
          const wasProcessed = transformer.process();
          if (wasProcessed) {
            return;
          }
        }
        this.tokens.copyToken();
      }
      /**
       * Skip past a class with a name and return that name.
       */
      processNamedClass() {
        if (!this.tokens.matches2(_types.TokenType._class, _types.TokenType.name)) {
          throw new Error("Expected identifier for exported class name.");
        }
        const name = this.tokens.identifierNameAtIndex(this.tokens.currentIndex() + 1);
        this.processClass();
        return name;
      }
      processClass() {
        const classInfo = _getClassInfo2.default.call(void 0, this, this.tokens, this.nameManager, this.disableESTransforms);
        const needsCommaExpression = (classInfo.headerInfo.isExpression || !classInfo.headerInfo.className) && classInfo.staticInitializerNames.length + classInfo.instanceInitializerNames.length > 0;
        let className = classInfo.headerInfo.className;
        if (needsCommaExpression) {
          className = this.nameManager.claimFreeName("_class");
          this.generatedVariables.push(className);
          this.tokens.appendCode(` (${className} =`);
        }
        const classToken = this.tokens.currentToken();
        const contextId = classToken.contextId;
        if (contextId == null) {
          throw new Error("Expected class to have a context ID.");
        }
        this.tokens.copyExpectedToken(_types.TokenType._class);
        while (!this.tokens.matchesContextIdAndLabel(_types.TokenType.braceL, contextId)) {
          this.processToken();
        }
        this.processClassBody(classInfo, className);
        const staticInitializerStatements = classInfo.staticInitializerNames.map(
          (name) => `${className}.${name}()`
        );
        if (needsCommaExpression) {
          this.tokens.appendCode(
            `, ${staticInitializerStatements.map((s) => `${s}, `).join("")}${className})`
          );
        } else if (classInfo.staticInitializerNames.length > 0) {
          this.tokens.appendCode(` ${staticInitializerStatements.map((s) => `${s};`).join(" ")}`);
        }
      }
      /**
       * We want to just handle class fields in all contexts, since TypeScript supports them. Later,
       * when some JS implementations support class fields, this should be made optional.
       */
      processClassBody(classInfo, className) {
        const {
          headerInfo,
          constructorInsertPos,
          constructorInitializerStatements,
          fields,
          instanceInitializerNames,
          rangesToRemove
        } = classInfo;
        let fieldIndex = 0;
        let rangeToRemoveIndex = 0;
        const classContextId = this.tokens.currentToken().contextId;
        if (classContextId == null) {
          throw new Error("Expected non-null context ID on class.");
        }
        this.tokens.copyExpectedToken(_types.TokenType.braceL);
        if (this.isReactHotLoaderTransformEnabled) {
          this.tokens.appendCode(
            "__reactstandin__regenerateByEval(key, code) {this[key] = eval(code);}"
          );
        }
        const needsConstructorInit = constructorInitializerStatements.length + instanceInitializerNames.length > 0;
        if (constructorInsertPos === null && needsConstructorInit) {
          const constructorInitializersCode = this.makeConstructorInitCode(
            constructorInitializerStatements,
            instanceInitializerNames,
            className
          );
          if (headerInfo.hasSuperclass) {
            const argsName = this.nameManager.claimFreeName("args");
            this.tokens.appendCode(
              `constructor(...${argsName}) { super(...${argsName}); ${constructorInitializersCode}; }`
            );
          } else {
            this.tokens.appendCode(`constructor() { ${constructorInitializersCode}; }`);
          }
        }
        while (!this.tokens.matchesContextIdAndLabel(_types.TokenType.braceR, classContextId)) {
          if (fieldIndex < fields.length && this.tokens.currentIndex() === fields[fieldIndex].start) {
            let needsCloseBrace = false;
            if (this.tokens.matches1(_types.TokenType.bracketL)) {
              this.tokens.copyTokenWithPrefix(`${fields[fieldIndex].initializerName}() {this`);
            } else if (this.tokens.matches1(_types.TokenType.string) || this.tokens.matches1(_types.TokenType.num)) {
              this.tokens.copyTokenWithPrefix(`${fields[fieldIndex].initializerName}() {this[`);
              needsCloseBrace = true;
            } else {
              this.tokens.copyTokenWithPrefix(`${fields[fieldIndex].initializerName}() {this.`);
            }
            while (this.tokens.currentIndex() < fields[fieldIndex].end) {
              if (needsCloseBrace && this.tokens.currentIndex() === fields[fieldIndex].equalsIndex) {
                this.tokens.appendCode("]");
              }
              this.processToken();
            }
            this.tokens.appendCode("}");
            fieldIndex++;
          } else if (rangeToRemoveIndex < rangesToRemove.length && this.tokens.currentIndex() >= rangesToRemove[rangeToRemoveIndex].start) {
            if (this.tokens.currentIndex() < rangesToRemove[rangeToRemoveIndex].end) {
              this.tokens.removeInitialToken();
            }
            while (this.tokens.currentIndex() < rangesToRemove[rangeToRemoveIndex].end) {
              this.tokens.removeToken();
            }
            rangeToRemoveIndex++;
          } else if (this.tokens.currentIndex() === constructorInsertPos) {
            this.tokens.copyToken();
            if (needsConstructorInit) {
              this.tokens.appendCode(
                `;${this.makeConstructorInitCode(
                  constructorInitializerStatements,
                  instanceInitializerNames,
                  className
                )};`
              );
            }
            this.processToken();
          } else {
            this.processToken();
          }
        }
        this.tokens.copyExpectedToken(_types.TokenType.braceR);
      }
      makeConstructorInitCode(constructorInitializerStatements, instanceInitializerNames, className) {
        return [
          ...constructorInitializerStatements,
          ...instanceInitializerNames.map((name) => `${className}.prototype.${name}.call(this)`)
        ].join(";");
      }
      /**
       * Normally it's ok to simply remove type tokens, but we need to be more careful when dealing with
       * arrow function return types since they can confuse the parser. In that case, we want to move
       * the close-paren to the same line as the arrow.
       *
       * See https://github.com/alangpierce/sucrase/issues/391 for more details.
       */
      processPossibleArrowParamEnd() {
        if (this.tokens.matches2(_types.TokenType.parenR, _types.TokenType.colon) && this.tokens.tokenAtRelativeIndex(1).isType) {
          let nextNonTypeIndex = this.tokens.currentIndex() + 1;
          while (this.tokens.tokens[nextNonTypeIndex].isType) {
            nextNonTypeIndex++;
          }
          if (this.tokens.matches1AtIndex(nextNonTypeIndex, _types.TokenType.arrow)) {
            this.tokens.removeInitialToken();
            while (this.tokens.currentIndex() < nextNonTypeIndex) {
              this.tokens.removeToken();
            }
            this.tokens.replaceTokenTrimmingLeftWhitespace(") =>");
            return true;
          }
        }
        return false;
      }
      /**
       * An async arrow function might be of the form:
       *
       * async <
       *   T
       * >() => {}
       *
       * in which case, removing the type parameters will cause a syntax error. Detect this case and
       * move the open-paren earlier.
       */
      processPossibleAsyncArrowWithTypeParams() {
        if (!this.tokens.matchesContextual(_keywords.ContextualKeyword._async) && !this.tokens.matches1(_types.TokenType._async)) {
          return false;
        }
        const nextToken = this.tokens.tokenAtRelativeIndex(1);
        if (nextToken.type !== _types.TokenType.lessThan || !nextToken.isType) {
          return false;
        }
        let nextNonTypeIndex = this.tokens.currentIndex() + 1;
        while (this.tokens.tokens[nextNonTypeIndex].isType) {
          nextNonTypeIndex++;
        }
        if (this.tokens.matches1AtIndex(nextNonTypeIndex, _types.TokenType.parenL)) {
          this.tokens.replaceToken("async (");
          this.tokens.removeInitialToken();
          while (this.tokens.currentIndex() < nextNonTypeIndex) {
            this.tokens.removeToken();
          }
          this.tokens.removeToken();
          this.processBalancedCode();
          this.processToken();
          return true;
        }
        return false;
      }
      processPossibleTypeRange() {
        if (this.tokens.currentToken().isType) {
          this.tokens.removeInitialToken();
          while (this.tokens.currentToken().isType) {
            this.tokens.removeToken();
          }
          return true;
        }
        return false;
      }
      shiftMappings(mappings, prefixLength) {
        for (let i = 0; i < mappings.length; i++) {
          const mapping = mappings[i];
          if (mapping !== void 0) {
            mappings[i] = mapping + prefixLength;
          }
        }
        return mappings;
      }
    };
    exports.default = RootTransformer;
  }
});

// node_modules/lines-and-columns/build/index.js
var require_build = __commonJS({
  "node_modules/lines-and-columns/build/index.js"(exports) {
    "use strict";
    exports.__esModule = true;
    exports.LinesAndColumns = void 0;
    var LF = "\n";
    var CR = "\r";
    var LinesAndColumns = (
      /** @class */
      function() {
        function LinesAndColumns2(string) {
          this.string = string;
          var offsets = [0];
          for (var offset2 = 0; offset2 < string.length; ) {
            switch (string[offset2]) {
              case LF:
                offset2 += LF.length;
                offsets.push(offset2);
                break;
              case CR:
                offset2 += CR.length;
                if (string[offset2] === LF) {
                  offset2 += LF.length;
                }
                offsets.push(offset2);
                break;
              default:
                offset2++;
                break;
            }
          }
          this.offsets = offsets;
        }
        LinesAndColumns2.prototype.locationForIndex = function(index) {
          if (index < 0 || index > this.string.length) {
            return null;
          }
          var line = 0;
          var offsets = this.offsets;
          while (offsets[line + 1] <= index) {
            line++;
          }
          var column = index - offsets[line];
          return { line, column };
        };
        LinesAndColumns2.prototype.indexForLocation = function(location) {
          var line = location.line, column = location.column;
          if (line < 0 || line >= this.offsets.length) {
            return null;
          }
          if (column < 0 || column > this.lengthOfLine(line)) {
            return null;
          }
          return this.offsets[line] + column;
        };
        LinesAndColumns2.prototype.lengthOfLine = function(line) {
          var offset2 = this.offsets[line];
          var nextOffset = line === this.offsets.length - 1 ? this.string.length : this.offsets[line + 1];
          return nextOffset - offset2;
        };
        return LinesAndColumns2;
      }()
    );
    exports.LinesAndColumns = LinesAndColumns;
    exports["default"] = LinesAndColumns;
  }
});

// node_modules/sucrase/dist/util/formatTokens.js
var require_formatTokens = __commonJS({
  "node_modules/sucrase/dist/util/formatTokens.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _linesandcolumns = require_build();
    var _linesandcolumns2 = _interopRequireDefault(_linesandcolumns);
    var _types = require_types();
    function formatTokens(code, tokens) {
      if (tokens.length === 0) {
        return "";
      }
      const tokenKeys = Object.keys(tokens[0]).filter(
        (k) => k !== "type" && k !== "value" && k !== "start" && k !== "end" && k !== "loc"
      );
      const typeKeys = Object.keys(tokens[0].type).filter((k) => k !== "label" && k !== "keyword");
      const headings = ["Location", "Label", "Raw", ...tokenKeys, ...typeKeys];
      const lines = new (0, _linesandcolumns2.default)(code);
      const rows = [headings, ...tokens.map(getTokenComponents)];
      const padding = headings.map(() => 0);
      for (const components of rows) {
        for (let i = 0; i < components.length; i++) {
          padding[i] = Math.max(padding[i], components[i].length);
        }
      }
      return rows.map((components) => components.map((component, i) => component.padEnd(padding[i])).join(" ")).join("\n");
      function getTokenComponents(token) {
        const raw = code.slice(token.start, token.end);
        return [
          formatRange(token.start, token.end),
          _types.formatTokenType.call(void 0, token.type),
          truncate(String(raw), 14),
          // @ts-ignore: Intentional dynamic access by key.
          ...tokenKeys.map((key) => formatValue(token[key], key)),
          // @ts-ignore: Intentional dynamic access by key.
          ...typeKeys.map((key) => formatValue(token.type[key], key))
        ];
      }
      function formatValue(value, key) {
        if (value === true) {
          return key;
        } else if (value === false || value === null) {
          return "";
        } else {
          return String(value);
        }
      }
      function formatRange(start, end) {
        return `${formatPos(start)}-${formatPos(end)}`;
      }
      function formatPos(pos) {
        const location = lines.locationForIndex(pos);
        if (!location) {
          return "Unknown";
        } else {
          return `${location.line + 1}:${location.column + 1}`;
        }
      }
    }
    exports.default = formatTokens;
    function truncate(s, length) {
      if (s.length > length) {
        return `${s.slice(0, length - 3)}...`;
      } else {
        return s;
      }
    }
  }
});

// node_modules/sucrase/dist/util/getTSImportedNames.js
var require_getTSImportedNames = __commonJS({
  "node_modules/sucrase/dist/util/getTSImportedNames.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _types = require_types();
    var _getImportExportSpecifierInfo = require_getImportExportSpecifierInfo();
    var _getImportExportSpecifierInfo2 = _interopRequireDefault(_getImportExportSpecifierInfo);
    function getTSImportedNames(tokens) {
      const importedNames = /* @__PURE__ */ new Set();
      for (let i = 0; i < tokens.tokens.length; i++) {
        if (tokens.matches1AtIndex(i, _types.TokenType._import) && !tokens.matches3AtIndex(i, _types.TokenType._import, _types.TokenType.name, _types.TokenType.eq)) {
          collectNamesForImport(tokens, i, importedNames);
        }
      }
      return importedNames;
    }
    exports.default = getTSImportedNames;
    function collectNamesForImport(tokens, index, importedNames) {
      index++;
      if (tokens.matches1AtIndex(index, _types.TokenType.parenL)) {
        return;
      }
      if (tokens.matches1AtIndex(index, _types.TokenType.name)) {
        importedNames.add(tokens.identifierNameAtIndex(index));
        index++;
        if (tokens.matches1AtIndex(index, _types.TokenType.comma)) {
          index++;
        }
      }
      if (tokens.matches1AtIndex(index, _types.TokenType.star)) {
        index += 2;
        importedNames.add(tokens.identifierNameAtIndex(index));
        index++;
      }
      if (tokens.matches1AtIndex(index, _types.TokenType.braceL)) {
        index++;
        collectNamesForNamedImport(tokens, index, importedNames);
      }
    }
    function collectNamesForNamedImport(tokens, index, importedNames) {
      while (true) {
        if (tokens.matches1AtIndex(index, _types.TokenType.braceR)) {
          return;
        }
        const specifierInfo = _getImportExportSpecifierInfo2.default.call(void 0, tokens, index);
        index = specifierInfo.endIndex;
        if (!specifierInfo.isType) {
          importedNames.add(specifierInfo.rightName);
        }
        if (tokens.matches2AtIndex(index, _types.TokenType.comma, _types.TokenType.braceR)) {
          return;
        } else if (tokens.matches1AtIndex(index, _types.TokenType.braceR)) {
          return;
        } else if (tokens.matches1AtIndex(index, _types.TokenType.comma)) {
          index++;
        } else {
          throw new Error(`Unexpected token: ${JSON.stringify(tokens.tokens[index])}`);
        }
      }
    }
  }
});

// node_modules/sucrase/dist/index.js
var require_dist2 = __commonJS({
  "node_modules/sucrase/dist/index.js"(exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    function _interopRequireDefault(obj) {
      return obj && obj.__esModule ? obj : { default: obj };
    }
    var _CJSImportProcessor = require_CJSImportProcessor();
    var _CJSImportProcessor2 = _interopRequireDefault(_CJSImportProcessor);
    var _computeSourceMap = require_computeSourceMap();
    var _computeSourceMap2 = _interopRequireDefault(_computeSourceMap);
    var _HelperManager = require_HelperManager();
    var _identifyShadowedGlobals = require_identifyShadowedGlobals();
    var _identifyShadowedGlobals2 = _interopRequireDefault(_identifyShadowedGlobals);
    var _NameManager = require_NameManager();
    var _NameManager2 = _interopRequireDefault(_NameManager);
    var _Options = require_Options();
    var _parser = require_parser();
    var _TokenProcessor = require_TokenProcessor();
    var _TokenProcessor2 = _interopRequireDefault(_TokenProcessor);
    var _RootTransformer = require_RootTransformer();
    var _RootTransformer2 = _interopRequireDefault(_RootTransformer);
    var _formatTokens = require_formatTokens();
    var _formatTokens2 = _interopRequireDefault(_formatTokens);
    var _getTSImportedNames = require_getTSImportedNames();
    var _getTSImportedNames2 = _interopRequireDefault(_getTSImportedNames);
    function getVersion() {
      return "3.35.0";
    }
    exports.getVersion = getVersion;
    function transform2(code, options) {
      _Options.validateOptions.call(void 0, options);
      try {
        const sucraseContext = getSucraseContext(code, options);
        const transformer = new (0, _RootTransformer2.default)(
          sucraseContext,
          options.transforms,
          Boolean(options.enableLegacyBabel5ModuleInterop),
          options
        );
        const transformerResult = transformer.transform();
        let result = { code: transformerResult.code };
        if (options.sourceMapOptions) {
          if (!options.filePath) {
            throw new Error("filePath must be specified when generating a source map.");
          }
          result = {
            ...result,
            sourceMap: _computeSourceMap2.default.call(
              void 0,
              transformerResult,
              options.filePath,
              options.sourceMapOptions,
              code,
              sucraseContext.tokenProcessor.tokens
            )
          };
        }
        return result;
      } catch (e) {
        if (options.filePath) {
          e.message = `Error transforming ${options.filePath}: ${e.message}`;
        }
        throw e;
      }
    }
    exports.transform = transform2;
    function getFormattedTokens(code, options) {
      const tokens = getSucraseContext(code, options).tokenProcessor.tokens;
      return _formatTokens2.default.call(void 0, code, tokens);
    }
    exports.getFormattedTokens = getFormattedTokens;
    function getSucraseContext(code, options) {
      const isJSXEnabled = options.transforms.includes("jsx");
      const isTypeScriptEnabled = options.transforms.includes("typescript");
      const isFlowEnabled = options.transforms.includes("flow");
      const disableESTransforms = options.disableESTransforms === true;
      const file2 = _parser.parse.call(void 0, code, isJSXEnabled, isTypeScriptEnabled, isFlowEnabled);
      const tokens = file2.tokens;
      const scopes = file2.scopes;
      const nameManager = new (0, _NameManager2.default)(code, tokens);
      const helperManager = new (0, _HelperManager.HelperManager)(nameManager);
      const tokenProcessor = new (0, _TokenProcessor2.default)(
        code,
        tokens,
        isFlowEnabled,
        disableESTransforms,
        helperManager
      );
      const enableLegacyTypeScriptModuleInterop = Boolean(options.enableLegacyTypeScriptModuleInterop);
      let importProcessor = null;
      if (options.transforms.includes("imports")) {
        importProcessor = new (0, _CJSImportProcessor2.default)(
          nameManager,
          tokenProcessor,
          enableLegacyTypeScriptModuleInterop,
          options,
          options.transforms.includes("typescript"),
          Boolean(options.keepUnusedImports),
          helperManager
        );
        importProcessor.preprocessTokens();
        _identifyShadowedGlobals2.default.call(void 0, tokenProcessor, scopes, importProcessor.getGlobalNames());
        if (options.transforms.includes("typescript") && !options.keepUnusedImports) {
          importProcessor.pruneTypeOnlyImports();
        }
      } else if (options.transforms.includes("typescript") && !options.keepUnusedImports) {
        _identifyShadowedGlobals2.default.call(void 0, tokenProcessor, scopes, _getTSImportedNames2.default.call(void 0, tokenProcessor));
      }
      return { tokenProcessor, scopes, nameManager, importProcessor, helperManager };
    }
  }
});

// lint-js.mjs
import * as std from "std";

// node_modules/acorn/dist/acorn.mjs
var astralIdentifierCodes = [509, 0, 227, 0, 150, 4, 294, 9, 1368, 2, 2, 1, 6, 3, 41, 2, 5, 0, 166, 1, 574, 3, 9, 9, 7, 9, 32, 4, 318, 1, 80, 3, 71, 10, 50, 3, 123, 2, 54, 14, 32, 10, 3, 1, 11, 3, 46, 10, 8, 0, 46, 9, 7, 2, 37, 13, 2, 9, 6, 1, 45, 0, 13, 2, 49, 13, 9, 3, 2, 11, 83, 11, 7, 0, 3, 0, 158, 11, 6, 9, 7, 3, 56, 1, 2, 6, 3, 1, 3, 2, 10, 0, 11, 1, 3, 6, 4, 4, 68, 8, 2, 0, 3, 0, 2, 3, 2, 4, 2, 0, 15, 1, 83, 17, 10, 9, 5, 0, 82, 19, 13, 9, 214, 6, 3, 8, 28, 1, 83, 16, 16, 9, 82, 12, 9, 9, 7, 19, 58, 14, 5, 9, 243, 14, 166, 9, 71, 5, 2, 1, 3, 3, 2, 0, 2, 1, 13, 9, 120, 6, 3, 6, 4, 0, 29, 9, 41, 6, 2, 3, 9, 0, 10, 10, 47, 15, 343, 9, 54, 7, 2, 7, 17, 9, 57, 21, 2, 13, 123, 5, 4, 0, 2, 1, 2, 6, 2, 0, 9, 9, 49, 4, 2, 1, 2, 4, 9, 9, 330, 3, 10, 1, 2, 0, 49, 6, 4, 4, 14, 10, 5350, 0, 7, 14, 11465, 27, 2343, 9, 87, 9, 39, 4, 60, 6, 26, 9, 535, 9, 470, 0, 2, 54, 8, 3, 82, 0, 12, 1, 19628, 1, 4178, 9, 519, 45, 3, 22, 543, 4, 4, 5, 9, 7, 3, 6, 31, 3, 149, 2, 1418, 49, 513, 54, 5, 49, 9, 0, 15, 0, 23, 4, 2, 14, 1361, 6, 2, 16, 3, 6, 2, 1, 2, 4, 101, 0, 161, 6, 10, 9, 357, 0, 62, 13, 499, 13, 245, 1, 2, 9, 726, 6, 110, 6, 6, 9, 4759, 9, 787719, 239];
var astralIdentifierStartCodes = [0, 11, 2, 25, 2, 18, 2, 1, 2, 14, 3, 13, 35, 122, 70, 52, 268, 28, 4, 48, 48, 31, 14, 29, 6, 37, 11, 29, 3, 35, 5, 7, 2, 4, 43, 157, 19, 35, 5, 35, 5, 39, 9, 51, 13, 10, 2, 14, 2, 6, 2, 1, 2, 10, 2, 14, 2, 6, 2, 1, 4, 51, 13, 310, 10, 21, 11, 7, 25, 5, 2, 41, 2, 8, 70, 5, 3, 0, 2, 43, 2, 1, 4, 0, 3, 22, 11, 22, 10, 30, 66, 18, 2, 1, 11, 21, 11, 25, 71, 55, 7, 1, 65, 0, 16, 3, 2, 2, 2, 28, 43, 28, 4, 28, 36, 7, 2, 27, 28, 53, 11, 21, 11, 18, 14, 17, 111, 72, 56, 50, 14, 50, 14, 35, 39, 27, 10, 22, 251, 41, 7, 1, 17, 2, 60, 28, 11, 0, 9, 21, 43, 17, 47, 20, 28, 22, 13, 52, 58, 1, 3, 0, 14, 44, 33, 24, 27, 35, 30, 0, 3, 0, 9, 34, 4, 0, 13, 47, 15, 3, 22, 0, 2, 0, 36, 17, 2, 24, 20, 1, 64, 6, 2, 0, 2, 3, 2, 14, 2, 9, 8, 46, 39, 7, 3, 1, 3, 21, 2, 6, 2, 1, 2, 4, 4, 0, 19, 0, 13, 4, 31, 9, 2, 0, 3, 0, 2, 37, 2, 0, 26, 0, 2, 0, 45, 52, 19, 3, 21, 2, 31, 47, 21, 1, 2, 0, 185, 46, 42, 3, 37, 47, 21, 0, 60, 42, 14, 0, 72, 26, 38, 6, 186, 43, 117, 63, 32, 7, 3, 0, 3, 7, 2, 1, 2, 23, 16, 0, 2, 0, 95, 7, 3, 38, 17, 0, 2, 0, 29, 0, 11, 39, 8, 0, 22, 0, 12, 45, 20, 0, 19, 72, 200, 32, 32, 8, 2, 36, 18, 0, 50, 29, 113, 6, 2, 1, 2, 37, 22, 0, 26, 5, 2, 1, 2, 31, 15, 0, 328, 18, 16, 0, 2, 12, 2, 33, 125, 0, 80, 921, 103, 110, 18, 195, 2637, 96, 16, 1071, 18, 5, 26, 3994, 6, 582, 6842, 29, 1763, 568, 8, 30, 18, 78, 18, 29, 19, 47, 17, 3, 32, 20, 6, 18, 433, 44, 212, 63, 129, 74, 6, 0, 67, 12, 65, 1, 2, 0, 29, 6135, 9, 1237, 42, 9, 8936, 3, 2, 6, 2, 1, 2, 290, 16, 0, 30, 2, 3, 0, 15, 3, 9, 395, 2309, 106, 6, 12, 4, 8, 8, 9, 5991, 84, 2, 70, 2, 1, 3, 0, 3, 1, 3, 3, 2, 11, 2, 0, 2, 6, 2, 64, 2, 3, 3, 7, 2, 6, 2, 27, 2, 3, 2, 4, 2, 0, 4, 6, 2, 339, 3, 24, 2, 24, 2, 30, 2, 24, 2, 30, 2, 24, 2, 30, 2, 24, 2, 30, 2, 24, 2, 7, 1845, 30, 7, 5, 262, 61, 147, 44, 11, 6, 17, 0, 322, 29, 19, 43, 485, 27, 229, 29, 3, 0, 496, 6, 2, 3, 2, 1, 2, 14, 2, 196, 60, 67, 8, 0, 1205, 3, 2, 26, 2, 1, 2, 0, 3, 0, 2, 9, 2, 3, 2, 0, 2, 0, 7, 0, 5, 0, 2, 0, 2, 0, 2, 2, 2, 1, 2, 0, 3, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 1, 2, 0, 3, 3, 2, 6, 2, 3, 2, 3, 2, 0, 2, 9, 2, 16, 6, 2, 2, 4, 2, 16, 4421, 42719, 33, 4153, 7, 221, 3, 5761, 15, 7472, 16, 621, 2467, 541, 1507, 4938, 6, 4191];
var nonASCIIidentifierChars = "\u200C\u200D\xB7\u0300-\u036F\u0387\u0483-\u0487\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7\u0610-\u061A\u064B-\u0669\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED\u06F0-\u06F9\u0711\u0730-\u074A\u07A6-\u07B0\u07C0-\u07C9\u07EB-\u07F3\u07FD\u0816-\u0819\u081B-\u0823\u0825-\u0827\u0829-\u082D\u0859-\u085B\u0897-\u089F\u08CA-\u08E1\u08E3-\u0903\u093A-\u093C\u093E-\u094F\u0951-\u0957\u0962\u0963\u0966-\u096F\u0981-\u0983\u09BC\u09BE-\u09C4\u09C7\u09C8\u09CB-\u09CD\u09D7\u09E2\u09E3\u09E6-\u09EF\u09FE\u0A01-\u0A03\u0A3C\u0A3E-\u0A42\u0A47\u0A48\u0A4B-\u0A4D\u0A51\u0A66-\u0A71\u0A75\u0A81-\u0A83\u0ABC\u0ABE-\u0AC5\u0AC7-\u0AC9\u0ACB-\u0ACD\u0AE2\u0AE3\u0AE6-\u0AEF\u0AFA-\u0AFF\u0B01-\u0B03\u0B3C\u0B3E-\u0B44\u0B47\u0B48\u0B4B-\u0B4D\u0B55-\u0B57\u0B62\u0B63\u0B66-\u0B6F\u0B82\u0BBE-\u0BC2\u0BC6-\u0BC8\u0BCA-\u0BCD\u0BD7\u0BE6-\u0BEF\u0C00-\u0C04\u0C3C\u0C3E-\u0C44\u0C46-\u0C48\u0C4A-\u0C4D\u0C55\u0C56\u0C62\u0C63\u0C66-\u0C6F\u0C81-\u0C83\u0CBC\u0CBE-\u0CC4\u0CC6-\u0CC8\u0CCA-\u0CCD\u0CD5\u0CD6\u0CE2\u0CE3\u0CE6-\u0CEF\u0CF3\u0D00-\u0D03\u0D3B\u0D3C\u0D3E-\u0D44\u0D46-\u0D48\u0D4A-\u0D4D\u0D57\u0D62\u0D63\u0D66-\u0D6F\u0D81-\u0D83\u0DCA\u0DCF-\u0DD4\u0DD6\u0DD8-\u0DDF\u0DE6-\u0DEF\u0DF2\u0DF3\u0E31\u0E34-\u0E3A\u0E47-\u0E4E\u0E50-\u0E59\u0EB1\u0EB4-\u0EBC\u0EC8-\u0ECE\u0ED0-\u0ED9\u0F18\u0F19\u0F20-\u0F29\u0F35\u0F37\u0F39\u0F3E\u0F3F\u0F71-\u0F84\u0F86\u0F87\u0F8D-\u0F97\u0F99-\u0FBC\u0FC6\u102B-\u103E\u1040-\u1049\u1056-\u1059\u105E-\u1060\u1062-\u1064\u1067-\u106D\u1071-\u1074\u1082-\u108D\u108F-\u109D\u135D-\u135F\u1369-\u1371\u1712-\u1715\u1732-\u1734\u1752\u1753\u1772\u1773\u17B4-\u17D3\u17DD\u17E0-\u17E9\u180B-\u180D\u180F-\u1819\u18A9\u1920-\u192B\u1930-\u193B\u1946-\u194F\u19D0-\u19DA\u1A17-\u1A1B\u1A55-\u1A5E\u1A60-\u1A7C\u1A7F-\u1A89\u1A90-\u1A99\u1AB0-\u1ABD\u1ABF-\u1ACE\u1B00-\u1B04\u1B34-\u1B44\u1B50-\u1B59\u1B6B-\u1B73\u1B80-\u1B82\u1BA1-\u1BAD\u1BB0-\u1BB9\u1BE6-\u1BF3\u1C24-\u1C37\u1C40-\u1C49\u1C50-\u1C59\u1CD0-\u1CD2\u1CD4-\u1CE8\u1CED\u1CF4\u1CF7-\u1CF9\u1DC0-\u1DFF\u200C\u200D\u203F\u2040\u2054\u20D0-\u20DC\u20E1\u20E5-\u20F0\u2CEF-\u2CF1\u2D7F\u2DE0-\u2DFF\u302A-\u302F\u3099\u309A\u30FB\uA620-\uA629\uA66F\uA674-\uA67D\uA69E\uA69F\uA6F0\uA6F1\uA802\uA806\uA80B\uA823-\uA827\uA82C\uA880\uA881\uA8B4-\uA8C5\uA8D0-\uA8D9\uA8E0-\uA8F1\uA8FF-\uA909\uA926-\uA92D\uA947-\uA953\uA980-\uA983\uA9B3-\uA9C0\uA9D0-\uA9D9\uA9E5\uA9F0-\uA9F9\uAA29-\uAA36\uAA43\uAA4C\uAA4D\uAA50-\uAA59\uAA7B-\uAA7D\uAAB0\uAAB2-\uAAB4\uAAB7\uAAB8\uAABE\uAABF\uAAC1\uAAEB-\uAAEF\uAAF5\uAAF6\uABE3-\uABEA\uABEC\uABED\uABF0-\uABF9\uFB1E\uFE00-\uFE0F\uFE20-\uFE2F\uFE33\uFE34\uFE4D-\uFE4F\uFF10-\uFF19\uFF3F\uFF65";
var nonASCIIidentifierStartChars = "\xAA\xB5\xBA\xC0-\xD6\xD8-\xF6\xF8-\u02C1\u02C6-\u02D1\u02E0-\u02E4\u02EC\u02EE\u0370-\u0374\u0376\u0377\u037A-\u037D\u037F\u0386\u0388-\u038A\u038C\u038E-\u03A1\u03A3-\u03F5\u03F7-\u0481\u048A-\u052F\u0531-\u0556\u0559\u0560-\u0588\u05D0-\u05EA\u05EF-\u05F2\u0620-\u064A\u066E\u066F\u0671-\u06D3\u06D5\u06E5\u06E6\u06EE\u06EF\u06FA-\u06FC\u06FF\u0710\u0712-\u072F\u074D-\u07A5\u07B1\u07CA-\u07EA\u07F4\u07F5\u07FA\u0800-\u0815\u081A\u0824\u0828\u0840-\u0858\u0860-\u086A\u0870-\u0887\u0889-\u088E\u08A0-\u08C9\u0904-\u0939\u093D\u0950\u0958-\u0961\u0971-\u0980\u0985-\u098C\u098F\u0990\u0993-\u09A8\u09AA-\u09B0\u09B2\u09B6-\u09B9\u09BD\u09CE\u09DC\u09DD\u09DF-\u09E1\u09F0\u09F1\u09FC\u0A05-\u0A0A\u0A0F\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32\u0A33\u0A35\u0A36\u0A38\u0A39\u0A59-\u0A5C\u0A5E\u0A72-\u0A74\u0A85-\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0\u0AB2\u0AB3\u0AB5-\u0AB9\u0ABD\u0AD0\u0AE0\u0AE1\u0AF9\u0B05-\u0B0C\u0B0F\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32\u0B33\u0B35-\u0B39\u0B3D\u0B5C\u0B5D\u0B5F-\u0B61\u0B71\u0B83\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99\u0B9A\u0B9C\u0B9E\u0B9F\u0BA3\u0BA4\u0BA8-\u0BAA\u0BAE-\u0BB9\u0BD0\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C39\u0C3D\u0C58-\u0C5A\u0C5D\u0C60\u0C61\u0C80\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CBD\u0CDD\u0CDE\u0CE0\u0CE1\u0CF1\u0CF2\u0D04-\u0D0C\u0D0E-\u0D10\u0D12-\u0D3A\u0D3D\u0D4E\u0D54-\u0D56\u0D5F-\u0D61\u0D7A-\u0D7F\u0D85-\u0D96\u0D9A-\u0DB1\u0DB3-\u0DBB\u0DBD\u0DC0-\u0DC6\u0E01-\u0E30\u0E32\u0E33\u0E40-\u0E46\u0E81\u0E82\u0E84\u0E86-\u0E8A\u0E8C-\u0EA3\u0EA5\u0EA7-\u0EB0\u0EB2\u0EB3\u0EBD\u0EC0-\u0EC4\u0EC6\u0EDC-\u0EDF\u0F00\u0F40-\u0F47\u0F49-\u0F6C\u0F88-\u0F8C\u1000-\u102A\u103F\u1050-\u1055\u105A-\u105D\u1061\u1065\u1066\u106E-\u1070\u1075-\u1081\u108E\u10A0-\u10C5\u10C7\u10CD\u10D0-\u10FA\u10FC-\u1248\u124A-\u124D\u1250-\u1256\u1258\u125A-\u125D\u1260-\u1288\u128A-\u128D\u1290-\u12B0\u12B2-\u12B5\u12B8-\u12BE\u12C0\u12C2-\u12C5\u12C8-\u12D6\u12D8-\u1310\u1312-\u1315\u1318-\u135A\u1380-\u138F\u13A0-\u13F5\u13F8-\u13FD\u1401-\u166C\u166F-\u167F\u1681-\u169A\u16A0-\u16EA\u16EE-\u16F8\u1700-\u1711\u171F-\u1731\u1740-\u1751\u1760-\u176C\u176E-\u1770\u1780-\u17B3\u17D7\u17DC\u1820-\u1878\u1880-\u18A8\u18AA\u18B0-\u18F5\u1900-\u191E\u1950-\u196D\u1970-\u1974\u1980-\u19AB\u19B0-\u19C9\u1A00-\u1A16\u1A20-\u1A54\u1AA7\u1B05-\u1B33\u1B45-\u1B4C\u1B83-\u1BA0\u1BAE\u1BAF\u1BBA-\u1BE5\u1C00-\u1C23\u1C4D-\u1C4F\u1C5A-\u1C7D\u1C80-\u1C8A\u1C90-\u1CBA\u1CBD-\u1CBF\u1CE9-\u1CEC\u1CEE-\u1CF3\u1CF5\u1CF6\u1CFA\u1D00-\u1DBF\u1E00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC\u2071\u207F\u2090-\u209C\u2102\u2107\u210A-\u2113\u2115\u2118-\u211D\u2124\u2126\u2128\u212A-\u2139\u213C-\u213F\u2145-\u2149\u214E\u2160-\u2188\u2C00-\u2CE4\u2CEB-\u2CEE\u2CF2\u2CF3\u2D00-\u2D25\u2D27\u2D2D\u2D30-\u2D67\u2D6F\u2D80-\u2D96\u2DA0-\u2DA6\u2DA8-\u2DAE\u2DB0-\u2DB6\u2DB8-\u2DBE\u2DC0-\u2DC6\u2DC8-\u2DCE\u2DD0-\u2DD6\u2DD8-\u2DDE\u3005-\u3007\u3021-\u3029\u3031-\u3035\u3038-\u303C\u3041-\u3096\u309B-\u309F\u30A1-\u30FA\u30FC-\u30FF\u3105-\u312F\u3131-\u318E\u31A0-\u31BF\u31F0-\u31FF\u3400-\u4DBF\u4E00-\uA48C\uA4D0-\uA4FD\uA500-\uA60C\uA610-\uA61F\uA62A\uA62B\uA640-\uA66E\uA67F-\uA69D\uA6A0-\uA6EF\uA717-\uA71F\uA722-\uA788\uA78B-\uA7CD\uA7D0\uA7D1\uA7D3\uA7D5-\uA7DC\uA7F2-\uA801\uA803-\uA805\uA807-\uA80A\uA80C-\uA822\uA840-\uA873\uA882-\uA8B3\uA8F2-\uA8F7\uA8FB\uA8FD\uA8FE\uA90A-\uA925\uA930-\uA946\uA960-\uA97C\uA984-\uA9B2\uA9CF\uA9E0-\uA9E4\uA9E6-\uA9EF\uA9FA-\uA9FE\uAA00-\uAA28\uAA40-\uAA42\uAA44-\uAA4B\uAA60-\uAA76\uAA7A\uAA7E-\uAAAF\uAAB1\uAAB5\uAAB6\uAAB9-\uAABD\uAAC0\uAAC2\uAADB-\uAADD\uAAE0-\uAAEA\uAAF2-\uAAF4\uAB01-\uAB06\uAB09-\uAB0E\uAB11-\uAB16\uAB20-\uAB26\uAB28-\uAB2E\uAB30-\uAB5A\uAB5C-\uAB69\uAB70-\uABE2\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uF900-\uFA6D\uFA70-\uFAD9\uFB00-\uFB06\uFB13-\uFB17\uFB1D\uFB1F-\uFB28\uFB2A-\uFB36\uFB38-\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46-\uFBB1\uFBD3-\uFD3D\uFD50-\uFD8F\uFD92-\uFDC7\uFDF0-\uFDFB\uFE70-\uFE74\uFE76-\uFEFC\uFF21-\uFF3A\uFF41-\uFF5A\uFF66-\uFFBE\uFFC2-\uFFC7\uFFCA-\uFFCF\uFFD2-\uFFD7\uFFDA-\uFFDC";
var reservedWords = {
  3: "abstract boolean byte char class double enum export extends final float goto implements import int interface long native package private protected public short static super synchronized throws transient volatile",
  5: "class enum extends super const export import",
  6: "enum",
  strict: "implements interface let package private protected public static yield",
  strictBind: "eval arguments"
};
var ecma5AndLessKeywords = "break case catch continue debugger default do else finally for function if return switch throw try var while with null true false instanceof typeof void delete new in this";
var keywords$1 = {
  5: ecma5AndLessKeywords,
  "5module": ecma5AndLessKeywords + " export import",
  6: ecma5AndLessKeywords + " const class extends export import super"
};
var keywordRelationalOperator = /^in(stanceof)?$/;
var nonASCIIidentifierStart = new RegExp("[" + nonASCIIidentifierStartChars + "]");
var nonASCIIidentifier = new RegExp("[" + nonASCIIidentifierStartChars + nonASCIIidentifierChars + "]");
function isInAstralSet(code, set) {
  var pos = 65536;
  for (var i = 0; i < set.length; i += 2) {
    pos += set[i];
    if (pos > code) {
      return false;
    }
    pos += set[i + 1];
    if (pos >= code) {
      return true;
    }
  }
  return false;
}
function isIdentifierStart(code, astral) {
  if (code < 65) {
    return code === 36;
  }
  if (code < 91) {
    return true;
  }
  if (code < 97) {
    return code === 95;
  }
  if (code < 123) {
    return true;
  }
  if (code <= 65535) {
    return code >= 170 && nonASCIIidentifierStart.test(String.fromCharCode(code));
  }
  if (astral === false) {
    return false;
  }
  return isInAstralSet(code, astralIdentifierStartCodes);
}
function isIdentifierChar(code, astral) {
  if (code < 48) {
    return code === 36;
  }
  if (code < 58) {
    return true;
  }
  if (code < 65) {
    return false;
  }
  if (code < 91) {
    return true;
  }
  if (code < 97) {
    return code === 95;
  }
  if (code < 123) {
    return true;
  }
  if (code <= 65535) {
    return code >= 170 && nonASCIIidentifier.test(String.fromCharCode(code));
  }
  if (astral === false) {
    return false;
  }
  return isInAstralSet(code, astralIdentifierStartCodes) || isInAstralSet(code, astralIdentifierCodes);
}
var TokenType = function TokenType2(label, conf) {
  if (conf === void 0) conf = {};
  this.label = label;
  this.keyword = conf.keyword;
  this.beforeExpr = !!conf.beforeExpr;
  this.startsExpr = !!conf.startsExpr;
  this.isLoop = !!conf.isLoop;
  this.isAssign = !!conf.isAssign;
  this.prefix = !!conf.prefix;
  this.postfix = !!conf.postfix;
  this.binop = conf.binop || null;
  this.updateContext = null;
};
function binop(name, prec) {
  return new TokenType(name, { beforeExpr: true, binop: prec });
}
var beforeExpr = { beforeExpr: true };
var startsExpr = { startsExpr: true };
var keywords = {};
function kw(name, options) {
  if (options === void 0) options = {};
  options.keyword = name;
  return keywords[name] = new TokenType(name, options);
}
var types$1 = {
  num: new TokenType("num", startsExpr),
  regexp: new TokenType("regexp", startsExpr),
  string: new TokenType("string", startsExpr),
  name: new TokenType("name", startsExpr),
  privateId: new TokenType("privateId", startsExpr),
  eof: new TokenType("eof"),
  // Punctuation token types.
  bracketL: new TokenType("[", { beforeExpr: true, startsExpr: true }),
  bracketR: new TokenType("]"),
  braceL: new TokenType("{", { beforeExpr: true, startsExpr: true }),
  braceR: new TokenType("}"),
  parenL: new TokenType("(", { beforeExpr: true, startsExpr: true }),
  parenR: new TokenType(")"),
  comma: new TokenType(",", beforeExpr),
  semi: new TokenType(";", beforeExpr),
  colon: new TokenType(":", beforeExpr),
  dot: new TokenType("."),
  question: new TokenType("?", beforeExpr),
  questionDot: new TokenType("?."),
  arrow: new TokenType("=>", beforeExpr),
  template: new TokenType("template"),
  invalidTemplate: new TokenType("invalidTemplate"),
  ellipsis: new TokenType("...", beforeExpr),
  backQuote: new TokenType("`", startsExpr),
  dollarBraceL: new TokenType("${", { beforeExpr: true, startsExpr: true }),
  // Operators. These carry several kinds of properties to help the
  // parser use them properly (the presence of these properties is
  // what categorizes them as operators).
  //
  // `binop`, when present, specifies that this operator is a binary
  // operator, and will refer to its precedence.
  //
  // `prefix` and `postfix` mark the operator as a prefix or postfix
  // unary operator.
  //
  // `isAssign` marks all of `=`, `+=`, `-=` etcetera, which act as
  // binary operators with a very low precedence, that should result
  // in AssignmentExpression nodes.
  eq: new TokenType("=", { beforeExpr: true, isAssign: true }),
  assign: new TokenType("_=", { beforeExpr: true, isAssign: true }),
  incDec: new TokenType("++/--", { prefix: true, postfix: true, startsExpr: true }),
  prefix: new TokenType("!/~", { beforeExpr: true, prefix: true, startsExpr: true }),
  logicalOR: binop("||", 1),
  logicalAND: binop("&&", 2),
  bitwiseOR: binop("|", 3),
  bitwiseXOR: binop("^", 4),
  bitwiseAND: binop("&", 5),
  equality: binop("==/!=/===/!==", 6),
  relational: binop("</>/<=/>=", 7),
  bitShift: binop("<</>>/>>>", 8),
  plusMin: new TokenType("+/-", { beforeExpr: true, binop: 9, prefix: true, startsExpr: true }),
  modulo: binop("%", 10),
  star: binop("*", 10),
  slash: binop("/", 10),
  starstar: new TokenType("**", { beforeExpr: true }),
  coalesce: binop("??", 1),
  // Keyword token types.
  _break: kw("break"),
  _case: kw("case", beforeExpr),
  _catch: kw("catch"),
  _continue: kw("continue"),
  _debugger: kw("debugger"),
  _default: kw("default", beforeExpr),
  _do: kw("do", { isLoop: true, beforeExpr: true }),
  _else: kw("else", beforeExpr),
  _finally: kw("finally"),
  _for: kw("for", { isLoop: true }),
  _function: kw("function", startsExpr),
  _if: kw("if"),
  _return: kw("return", beforeExpr),
  _switch: kw("switch"),
  _throw: kw("throw", beforeExpr),
  _try: kw("try"),
  _var: kw("var"),
  _const: kw("const"),
  _while: kw("while", { isLoop: true }),
  _with: kw("with"),
  _new: kw("new", { beforeExpr: true, startsExpr: true }),
  _this: kw("this", startsExpr),
  _super: kw("super", startsExpr),
  _class: kw("class", startsExpr),
  _extends: kw("extends", beforeExpr),
  _export: kw("export"),
  _import: kw("import", startsExpr),
  _null: kw("null", startsExpr),
  _true: kw("true", startsExpr),
  _false: kw("false", startsExpr),
  _in: kw("in", { beforeExpr: true, binop: 7 }),
  _instanceof: kw("instanceof", { beforeExpr: true, binop: 7 }),
  _typeof: kw("typeof", { beforeExpr: true, prefix: true, startsExpr: true }),
  _void: kw("void", { beforeExpr: true, prefix: true, startsExpr: true }),
  _delete: kw("delete", { beforeExpr: true, prefix: true, startsExpr: true })
};
var lineBreak = /\r\n?|\n|\u2028|\u2029/;
var lineBreakG = new RegExp(lineBreak.source, "g");
function isNewLine(code) {
  return code === 10 || code === 13 || code === 8232 || code === 8233;
}
function nextLineBreak(code, from, end) {
  if (end === void 0) end = code.length;
  for (var i = from; i < end; i++) {
    var next = code.charCodeAt(i);
    if (isNewLine(next)) {
      return i < end - 1 && next === 13 && code.charCodeAt(i + 1) === 10 ? i + 2 : i + 1;
    }
  }
  return -1;
}
var nonASCIIwhitespace = /[\u1680\u2000-\u200a\u202f\u205f\u3000\ufeff]/;
var skipWhiteSpace = /(?:\s|\/\/.*|\/\*[^]*?\*\/)*/g;
var ref = Object.prototype;
var hasOwnProperty = ref.hasOwnProperty;
var toString = ref.toString;
var hasOwn = Object.hasOwn || function(obj, propName) {
  return hasOwnProperty.call(obj, propName);
};
var isArray = Array.isArray || function(obj) {
  return toString.call(obj) === "[object Array]";
};
var regexpCache = /* @__PURE__ */ Object.create(null);
function wordsRegexp(words) {
  return regexpCache[words] || (regexpCache[words] = new RegExp("^(?:" + words.replace(/ /g, "|") + ")$"));
}
function codePointToString(code) {
  if (code <= 65535) {
    return String.fromCharCode(code);
  }
  code -= 65536;
  return String.fromCharCode((code >> 10) + 55296, (code & 1023) + 56320);
}
var loneSurrogate = /(?:[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?:[^\uD800-\uDBFF]|^)[\uDC00-\uDFFF])/;
var Position = function Position2(line, col) {
  this.line = line;
  this.column = col;
};
Position.prototype.offset = function offset(n) {
  return new Position(this.line, this.column + n);
};
var SourceLocation = function SourceLocation2(p, start, end) {
  this.start = start;
  this.end = end;
  if (p.sourceFile !== null) {
    this.source = p.sourceFile;
  }
};
function getLineInfo(input, offset2) {
  for (var line = 1, cur = 0; ; ) {
    var nextBreak = nextLineBreak(input, cur, offset2);
    if (nextBreak < 0) {
      return new Position(line, offset2 - cur);
    }
    ++line;
    cur = nextBreak;
  }
}
var defaultOptions = {
  // `ecmaVersion` indicates the ECMAScript version to parse. Must be
  // either 3, 5, 6 (or 2015), 7 (2016), 8 (2017), 9 (2018), 10
  // (2019), 11 (2020), 12 (2021), 13 (2022), 14 (2023), or `"latest"`
  // (the latest version the library supports). This influences
  // support for strict mode, the set of reserved words, and support
  // for new syntax features.
  ecmaVersion: null,
  // `sourceType` indicates the mode the code should be parsed in.
  // Can be either `"script"` or `"module"`. This influences global
  // strict mode and parsing of `import` and `export` declarations.
  sourceType: "script",
  // `onInsertedSemicolon` can be a callback that will be called when
  // a semicolon is automatically inserted. It will be passed the
  // position of the inserted semicolon as an offset, and if
  // `locations` is enabled, it is given the location as a `{line,
  // column}` object as second argument.
  onInsertedSemicolon: null,
  // `onTrailingComma` is similar to `onInsertedSemicolon`, but for
  // trailing commas.
  onTrailingComma: null,
  // By default, reserved words are only enforced if ecmaVersion >= 5.
  // Set `allowReserved` to a boolean value to explicitly turn this on
  // an off. When this option has the value "never", reserved words
  // and keywords can also not be used as property names.
  allowReserved: null,
  // When enabled, a return at the top level is not considered an
  // error.
  allowReturnOutsideFunction: false,
  // When enabled, import/export statements are not constrained to
  // appearing at the top of the program, and an import.meta expression
  // in a script isn't considered an error.
  allowImportExportEverywhere: false,
  // By default, await identifiers are allowed to appear at the top-level scope only if ecmaVersion >= 2022.
  // When enabled, await identifiers are allowed to appear at the top-level scope,
  // but they are still not allowed in non-async functions.
  allowAwaitOutsideFunction: null,
  // When enabled, super identifiers are not constrained to
  // appearing in methods and do not raise an error when they appear elsewhere.
  allowSuperOutsideMethod: null,
  // When enabled, hashbang directive in the beginning of file is
  // allowed and treated as a line comment. Enabled by default when
  // `ecmaVersion` >= 2023.
  allowHashBang: false,
  // By default, the parser will verify that private properties are
  // only used in places where they are valid and have been declared.
  // Set this to false to turn such checks off.
  checkPrivateFields: true,
  // When `locations` is on, `loc` properties holding objects with
  // `start` and `end` properties in `{line, column}` form (with
  // line being 1-based and column 0-based) will be attached to the
  // nodes.
  locations: false,
  // A function can be passed as `onToken` option, which will
  // cause Acorn to call that function with object in the same
  // format as tokens returned from `tokenizer().getToken()`. Note
  // that you are not allowed to call the parser from the
  // callback—that will corrupt its internal state.
  onToken: null,
  // A function can be passed as `onComment` option, which will
  // cause Acorn to call that function with `(block, text, start,
  // end)` parameters whenever a comment is skipped. `block` is a
  // boolean indicating whether this is a block (`/* */`) comment,
  // `text` is the content of the comment, and `start` and `end` are
  // character offsets that denote the start and end of the comment.
  // When the `locations` option is on, two more parameters are
  // passed, the full `{line, column}` locations of the start and
  // end of the comments. Note that you are not allowed to call the
  // parser from the callback—that will corrupt its internal state.
  // When this option has an array as value, objects representing the
  // comments are pushed to it.
  onComment: null,
  // Nodes have their start and end characters offsets recorded in
  // `start` and `end` properties (directly on the node, rather than
  // the `loc` object, which holds line/column data. To also add a
  // [semi-standardized][range] `range` property holding a `[start,
  // end]` array with the same numbers, set the `ranges` option to
  // `true`.
  //
  // [range]: https://bugzilla.mozilla.org/show_bug.cgi?id=745678
  ranges: false,
  // It is possible to parse multiple files into a single AST by
  // passing the tree produced by parsing the first file as
  // `program` option in subsequent parses. This will add the
  // toplevel forms of the parsed file to the `Program` (top) node
  // of an existing parse tree.
  program: null,
  // When `locations` is on, you can pass this to record the source
  // file in every node's `loc` object.
  sourceFile: null,
  // This value, if given, is stored in every node, whether
  // `locations` is on or off.
  directSourceFile: null,
  // When enabled, parenthesized expressions are represented by
  // (non-standard) ParenthesizedExpression nodes
  preserveParens: false
};
var warnedAboutEcmaVersion = false;
function getOptions(opts) {
  var options = {};
  for (var opt in defaultOptions) {
    options[opt] = opts && hasOwn(opts, opt) ? opts[opt] : defaultOptions[opt];
  }
  if (options.ecmaVersion === "latest") {
    options.ecmaVersion = 1e8;
  } else if (options.ecmaVersion == null) {
    if (!warnedAboutEcmaVersion && typeof console === "object" && console.warn) {
      warnedAboutEcmaVersion = true;
      console.warn("Since Acorn 8.0.0, options.ecmaVersion is required.\nDefaulting to 2020, but this will stop working in the future.");
    }
    options.ecmaVersion = 11;
  } else if (options.ecmaVersion >= 2015) {
    options.ecmaVersion -= 2009;
  }
  if (options.allowReserved == null) {
    options.allowReserved = options.ecmaVersion < 5;
  }
  if (!opts || opts.allowHashBang == null) {
    options.allowHashBang = options.ecmaVersion >= 14;
  }
  if (isArray(options.onToken)) {
    var tokens = options.onToken;
    options.onToken = function(token) {
      return tokens.push(token);
    };
  }
  if (isArray(options.onComment)) {
    options.onComment = pushComment(options, options.onComment);
  }
  return options;
}
function pushComment(options, array) {
  return function(block, text, start, end, startLoc, endLoc) {
    var comment = {
      type: block ? "Block" : "Line",
      value: text,
      start,
      end
    };
    if (options.locations) {
      comment.loc = new SourceLocation(this, startLoc, endLoc);
    }
    if (options.ranges) {
      comment.range = [start, end];
    }
    array.push(comment);
  };
}
var SCOPE_TOP = 1;
var SCOPE_FUNCTION = 2;
var SCOPE_ASYNC = 4;
var SCOPE_GENERATOR = 8;
var SCOPE_ARROW = 16;
var SCOPE_SIMPLE_CATCH = 32;
var SCOPE_SUPER = 64;
var SCOPE_DIRECT_SUPER = 128;
var SCOPE_CLASS_STATIC_BLOCK = 256;
var SCOPE_VAR = SCOPE_TOP | SCOPE_FUNCTION | SCOPE_CLASS_STATIC_BLOCK;
function functionFlags(async, generator) {
  return SCOPE_FUNCTION | (async ? SCOPE_ASYNC : 0) | (generator ? SCOPE_GENERATOR : 0);
}
var BIND_NONE = 0;
var BIND_VAR = 1;
var BIND_LEXICAL = 2;
var BIND_FUNCTION = 3;
var BIND_SIMPLE_CATCH = 4;
var BIND_OUTSIDE = 5;
var Parser = function Parser2(options, input, startPos) {
  this.options = options = getOptions(options);
  this.sourceFile = options.sourceFile;
  this.keywords = wordsRegexp(keywords$1[options.ecmaVersion >= 6 ? 6 : options.sourceType === "module" ? "5module" : 5]);
  var reserved = "";
  if (options.allowReserved !== true) {
    reserved = reservedWords[options.ecmaVersion >= 6 ? 6 : options.ecmaVersion === 5 ? 5 : 3];
    if (options.sourceType === "module") {
      reserved += " await";
    }
  }
  this.reservedWords = wordsRegexp(reserved);
  var reservedStrict = (reserved ? reserved + " " : "") + reservedWords.strict;
  this.reservedWordsStrict = wordsRegexp(reservedStrict);
  this.reservedWordsStrictBind = wordsRegexp(reservedStrict + " " + reservedWords.strictBind);
  this.input = String(input);
  this.containsEsc = false;
  if (startPos) {
    this.pos = startPos;
    this.lineStart = this.input.lastIndexOf("\n", startPos - 1) + 1;
    this.curLine = this.input.slice(0, this.lineStart).split(lineBreak).length;
  } else {
    this.pos = this.lineStart = 0;
    this.curLine = 1;
  }
  this.type = types$1.eof;
  this.value = null;
  this.start = this.end = this.pos;
  this.startLoc = this.endLoc = this.curPosition();
  this.lastTokEndLoc = this.lastTokStartLoc = null;
  this.lastTokStart = this.lastTokEnd = this.pos;
  this.context = this.initialContext();
  this.exprAllowed = true;
  this.inModule = options.sourceType === "module";
  this.strict = this.inModule || this.strictDirective(this.pos);
  this.potentialArrowAt = -1;
  this.potentialArrowInForAwait = false;
  this.yieldPos = this.awaitPos = this.awaitIdentPos = 0;
  this.labels = [];
  this.undefinedExports = /* @__PURE__ */ Object.create(null);
  if (this.pos === 0 && options.allowHashBang && this.input.slice(0, 2) === "#!") {
    this.skipLineComment(2);
  }
  this.scopeStack = [];
  this.enterScope(SCOPE_TOP);
  this.regexpState = null;
  this.privateNameStack = [];
};
var prototypeAccessors = { inFunction: { configurable: true }, inGenerator: { configurable: true }, inAsync: { configurable: true }, canAwait: { configurable: true }, allowSuper: { configurable: true }, allowDirectSuper: { configurable: true }, treatFunctionsAsVar: { configurable: true }, allowNewDotTarget: { configurable: true }, inClassStaticBlock: { configurable: true } };
Parser.prototype.parse = function parse() {
  var node = this.options.program || this.startNode();
  this.nextToken();
  return this.parseTopLevel(node);
};
prototypeAccessors.inFunction.get = function() {
  return (this.currentVarScope().flags & SCOPE_FUNCTION) > 0;
};
prototypeAccessors.inGenerator.get = function() {
  return (this.currentVarScope().flags & SCOPE_GENERATOR) > 0 && !this.currentVarScope().inClassFieldInit;
};
prototypeAccessors.inAsync.get = function() {
  return (this.currentVarScope().flags & SCOPE_ASYNC) > 0 && !this.currentVarScope().inClassFieldInit;
};
prototypeAccessors.canAwait.get = function() {
  for (var i = this.scopeStack.length - 1; i >= 0; i--) {
    var scope = this.scopeStack[i];
    if (scope.inClassFieldInit || scope.flags & SCOPE_CLASS_STATIC_BLOCK) {
      return false;
    }
    if (scope.flags & SCOPE_FUNCTION) {
      return (scope.flags & SCOPE_ASYNC) > 0;
    }
  }
  return this.inModule && this.options.ecmaVersion >= 13 || this.options.allowAwaitOutsideFunction;
};
prototypeAccessors.allowSuper.get = function() {
  var ref2 = this.currentThisScope();
  var flags = ref2.flags;
  var inClassFieldInit = ref2.inClassFieldInit;
  return (flags & SCOPE_SUPER) > 0 || inClassFieldInit || this.options.allowSuperOutsideMethod;
};
prototypeAccessors.allowDirectSuper.get = function() {
  return (this.currentThisScope().flags & SCOPE_DIRECT_SUPER) > 0;
};
prototypeAccessors.treatFunctionsAsVar.get = function() {
  return this.treatFunctionsAsVarInScope(this.currentScope());
};
prototypeAccessors.allowNewDotTarget.get = function() {
  var ref2 = this.currentThisScope();
  var flags = ref2.flags;
  var inClassFieldInit = ref2.inClassFieldInit;
  return (flags & (SCOPE_FUNCTION | SCOPE_CLASS_STATIC_BLOCK)) > 0 || inClassFieldInit;
};
prototypeAccessors.inClassStaticBlock.get = function() {
  return (this.currentVarScope().flags & SCOPE_CLASS_STATIC_BLOCK) > 0;
};
Parser.extend = function extend() {
  var plugins = [], len = arguments.length;
  while (len--) plugins[len] = arguments[len];
  var cls = this;
  for (var i = 0; i < plugins.length; i++) {
    cls = plugins[i](cls);
  }
  return cls;
};
Parser.parse = function parse2(input, options) {
  return new this(options, input).parse();
};
Parser.parseExpressionAt = function parseExpressionAt(input, pos, options) {
  var parser = new this(options, input, pos);
  parser.nextToken();
  return parser.parseExpression();
};
Parser.tokenizer = function tokenizer(input, options) {
  return new this(options, input);
};
Object.defineProperties(Parser.prototype, prototypeAccessors);
var pp$9 = Parser.prototype;
var literal = /^(?:'((?:\\[^]|[^'\\])*?)'|"((?:\\[^]|[^"\\])*?)")/;
pp$9.strictDirective = function(start) {
  if (this.options.ecmaVersion < 5) {
    return false;
  }
  for (; ; ) {
    skipWhiteSpace.lastIndex = start;
    start += skipWhiteSpace.exec(this.input)[0].length;
    var match = literal.exec(this.input.slice(start));
    if (!match) {
      return false;
    }
    if ((match[1] || match[2]) === "use strict") {
      skipWhiteSpace.lastIndex = start + match[0].length;
      var spaceAfter = skipWhiteSpace.exec(this.input), end = spaceAfter.index + spaceAfter[0].length;
      var next = this.input.charAt(end);
      return next === ";" || next === "}" || lineBreak.test(spaceAfter[0]) && !(/[(`.[+\-/*%<>=,?^&]/.test(next) || next === "!" && this.input.charAt(end + 1) === "=");
    }
    start += match[0].length;
    skipWhiteSpace.lastIndex = start;
    start += skipWhiteSpace.exec(this.input)[0].length;
    if (this.input[start] === ";") {
      start++;
    }
  }
};
pp$9.eat = function(type) {
  if (this.type === type) {
    this.next();
    return true;
  } else {
    return false;
  }
};
pp$9.isContextual = function(name) {
  return this.type === types$1.name && this.value === name && !this.containsEsc;
};
pp$9.eatContextual = function(name) {
  if (!this.isContextual(name)) {
    return false;
  }
  this.next();
  return true;
};
pp$9.expectContextual = function(name) {
  if (!this.eatContextual(name)) {
    this.unexpected();
  }
};
pp$9.canInsertSemicolon = function() {
  return this.type === types$1.eof || this.type === types$1.braceR || lineBreak.test(this.input.slice(this.lastTokEnd, this.start));
};
pp$9.insertSemicolon = function() {
  if (this.canInsertSemicolon()) {
    if (this.options.onInsertedSemicolon) {
      this.options.onInsertedSemicolon(this.lastTokEnd, this.lastTokEndLoc);
    }
    return true;
  }
};
pp$9.semicolon = function() {
  if (!this.eat(types$1.semi) && !this.insertSemicolon()) {
    this.unexpected();
  }
};
pp$9.afterTrailingComma = function(tokType, notNext) {
  if (this.type === tokType) {
    if (this.options.onTrailingComma) {
      this.options.onTrailingComma(this.lastTokStart, this.lastTokStartLoc);
    }
    if (!notNext) {
      this.next();
    }
    return true;
  }
};
pp$9.expect = function(type) {
  this.eat(type) || this.unexpected();
};
pp$9.unexpected = function(pos) {
  this.raise(pos != null ? pos : this.start, "Unexpected token");
};
var DestructuringErrors = function DestructuringErrors2() {
  this.shorthandAssign = this.trailingComma = this.parenthesizedAssign = this.parenthesizedBind = this.doubleProto = -1;
};
pp$9.checkPatternErrors = function(refDestructuringErrors, isAssign) {
  if (!refDestructuringErrors) {
    return;
  }
  if (refDestructuringErrors.trailingComma > -1) {
    this.raiseRecoverable(refDestructuringErrors.trailingComma, "Comma is not permitted after the rest element");
  }
  var parens = isAssign ? refDestructuringErrors.parenthesizedAssign : refDestructuringErrors.parenthesizedBind;
  if (parens > -1) {
    this.raiseRecoverable(parens, isAssign ? "Assigning to rvalue" : "Parenthesized pattern");
  }
};
pp$9.checkExpressionErrors = function(refDestructuringErrors, andThrow) {
  if (!refDestructuringErrors) {
    return false;
  }
  var shorthandAssign = refDestructuringErrors.shorthandAssign;
  var doubleProto = refDestructuringErrors.doubleProto;
  if (!andThrow) {
    return shorthandAssign >= 0 || doubleProto >= 0;
  }
  if (shorthandAssign >= 0) {
    this.raise(shorthandAssign, "Shorthand property assignments are valid only in destructuring patterns");
  }
  if (doubleProto >= 0) {
    this.raiseRecoverable(doubleProto, "Redefinition of __proto__ property");
  }
};
pp$9.checkYieldAwaitInDefaultParams = function() {
  if (this.yieldPos && (!this.awaitPos || this.yieldPos < this.awaitPos)) {
    this.raise(this.yieldPos, "Yield expression cannot be a default value");
  }
  if (this.awaitPos) {
    this.raise(this.awaitPos, "Await expression cannot be a default value");
  }
};
pp$9.isSimpleAssignTarget = function(expr) {
  if (expr.type === "ParenthesizedExpression") {
    return this.isSimpleAssignTarget(expr.expression);
  }
  return expr.type === "Identifier" || expr.type === "MemberExpression";
};
var pp$8 = Parser.prototype;
pp$8.parseTopLevel = function(node) {
  var exports = /* @__PURE__ */ Object.create(null);
  if (!node.body) {
    node.body = [];
  }
  while (this.type !== types$1.eof) {
    var stmt = this.parseStatement(null, true, exports);
    node.body.push(stmt);
  }
  if (this.inModule) {
    for (var i = 0, list = Object.keys(this.undefinedExports); i < list.length; i += 1) {
      var name = list[i];
      this.raiseRecoverable(this.undefinedExports[name].start, "Export '" + name + "' is not defined");
    }
  }
  this.adaptDirectivePrologue(node.body);
  this.next();
  node.sourceType = this.options.sourceType;
  return this.finishNode(node, "Program");
};
var loopLabel = { kind: "loop" };
var switchLabel = { kind: "switch" };
pp$8.isLet = function(context) {
  if (this.options.ecmaVersion < 6 || !this.isContextual("let")) {
    return false;
  }
  skipWhiteSpace.lastIndex = this.pos;
  var skip = skipWhiteSpace.exec(this.input);
  var next = this.pos + skip[0].length, nextCh = this.input.charCodeAt(next);
  if (nextCh === 91 || nextCh === 92) {
    return true;
  }
  if (context) {
    return false;
  }
  if (nextCh === 123 || nextCh > 55295 && nextCh < 56320) {
    return true;
  }
  if (isIdentifierStart(nextCh, true)) {
    var pos = next + 1;
    while (isIdentifierChar(nextCh = this.input.charCodeAt(pos), true)) {
      ++pos;
    }
    if (nextCh === 92 || nextCh > 55295 && nextCh < 56320) {
      return true;
    }
    var ident = this.input.slice(next, pos);
    if (!keywordRelationalOperator.test(ident)) {
      return true;
    }
  }
  return false;
};
pp$8.isAsyncFunction = function() {
  if (this.options.ecmaVersion < 8 || !this.isContextual("async")) {
    return false;
  }
  skipWhiteSpace.lastIndex = this.pos;
  var skip = skipWhiteSpace.exec(this.input);
  var next = this.pos + skip[0].length, after;
  return !lineBreak.test(this.input.slice(this.pos, next)) && this.input.slice(next, next + 8) === "function" && (next + 8 === this.input.length || !(isIdentifierChar(after = this.input.charCodeAt(next + 8)) || after > 55295 && after < 56320));
};
pp$8.parseStatement = function(context, topLevel, exports) {
  var starttype = this.type, node = this.startNode(), kind;
  if (this.isLet(context)) {
    starttype = types$1._var;
    kind = "let";
  }
  switch (starttype) {
    case types$1._break:
    case types$1._continue:
      return this.parseBreakContinueStatement(node, starttype.keyword);
    case types$1._debugger:
      return this.parseDebuggerStatement(node);
    case types$1._do:
      return this.parseDoStatement(node);
    case types$1._for:
      return this.parseForStatement(node);
    case types$1._function:
      if (context && (this.strict || context !== "if" && context !== "label") && this.options.ecmaVersion >= 6) {
        this.unexpected();
      }
      return this.parseFunctionStatement(node, false, !context);
    case types$1._class:
      if (context) {
        this.unexpected();
      }
      return this.parseClass(node, true);
    case types$1._if:
      return this.parseIfStatement(node);
    case types$1._return:
      return this.parseReturnStatement(node);
    case types$1._switch:
      return this.parseSwitchStatement(node);
    case types$1._throw:
      return this.parseThrowStatement(node);
    case types$1._try:
      return this.parseTryStatement(node);
    case types$1._const:
    case types$1._var:
      kind = kind || this.value;
      if (context && kind !== "var") {
        this.unexpected();
      }
      return this.parseVarStatement(node, kind);
    case types$1._while:
      return this.parseWhileStatement(node);
    case types$1._with:
      return this.parseWithStatement(node);
    case types$1.braceL:
      return this.parseBlock(true, node);
    case types$1.semi:
      return this.parseEmptyStatement(node);
    case types$1._export:
    case types$1._import:
      if (this.options.ecmaVersion > 10 && starttype === types$1._import) {
        skipWhiteSpace.lastIndex = this.pos;
        var skip = skipWhiteSpace.exec(this.input);
        var next = this.pos + skip[0].length, nextCh = this.input.charCodeAt(next);
        if (nextCh === 40 || nextCh === 46) {
          return this.parseExpressionStatement(node, this.parseExpression());
        }
      }
      if (!this.options.allowImportExportEverywhere) {
        if (!topLevel) {
          this.raise(this.start, "'import' and 'export' may only appear at the top level");
        }
        if (!this.inModule) {
          this.raise(this.start, "'import' and 'export' may appear only with 'sourceType: module'");
        }
      }
      return starttype === types$1._import ? this.parseImport(node) : this.parseExport(node, exports);
    // If the statement does not start with a statement keyword or a
    // brace, it's an ExpressionStatement or LabeledStatement. We
    // simply start parsing an expression, and afterwards, if the
    // next token is a colon and the expression was a simple
    // Identifier node, we switch to interpreting it as a label.
    default:
      if (this.isAsyncFunction()) {
        if (context) {
          this.unexpected();
        }
        this.next();
        return this.parseFunctionStatement(node, true, !context);
      }
      var maybeName = this.value, expr = this.parseExpression();
      if (starttype === types$1.name && expr.type === "Identifier" && this.eat(types$1.colon)) {
        return this.parseLabeledStatement(node, maybeName, expr, context);
      } else {
        return this.parseExpressionStatement(node, expr);
      }
  }
};
pp$8.parseBreakContinueStatement = function(node, keyword) {
  var isBreak = keyword === "break";
  this.next();
  if (this.eat(types$1.semi) || this.insertSemicolon()) {
    node.label = null;
  } else if (this.type !== types$1.name) {
    this.unexpected();
  } else {
    node.label = this.parseIdent();
    this.semicolon();
  }
  var i = 0;
  for (; i < this.labels.length; ++i) {
    var lab = this.labels[i];
    if (node.label == null || lab.name === node.label.name) {
      if (lab.kind != null && (isBreak || lab.kind === "loop")) {
        break;
      }
      if (node.label && isBreak) {
        break;
      }
    }
  }
  if (i === this.labels.length) {
    this.raise(node.start, "Unsyntactic " + keyword);
  }
  return this.finishNode(node, isBreak ? "BreakStatement" : "ContinueStatement");
};
pp$8.parseDebuggerStatement = function(node) {
  this.next();
  this.semicolon();
  return this.finishNode(node, "DebuggerStatement");
};
pp$8.parseDoStatement = function(node) {
  this.next();
  this.labels.push(loopLabel);
  node.body = this.parseStatement("do");
  this.labels.pop();
  this.expect(types$1._while);
  node.test = this.parseParenExpression();
  if (this.options.ecmaVersion >= 6) {
    this.eat(types$1.semi);
  } else {
    this.semicolon();
  }
  return this.finishNode(node, "DoWhileStatement");
};
pp$8.parseForStatement = function(node) {
  this.next();
  var awaitAt = this.options.ecmaVersion >= 9 && this.canAwait && this.eatContextual("await") ? this.lastTokStart : -1;
  this.labels.push(loopLabel);
  this.enterScope(0);
  this.expect(types$1.parenL);
  if (this.type === types$1.semi) {
    if (awaitAt > -1) {
      this.unexpected(awaitAt);
    }
    return this.parseFor(node, null);
  }
  var isLet = this.isLet();
  if (this.type === types$1._var || this.type === types$1._const || isLet) {
    var init$1 = this.startNode(), kind = isLet ? "let" : this.value;
    this.next();
    this.parseVar(init$1, true, kind);
    this.finishNode(init$1, "VariableDeclaration");
    if ((this.type === types$1._in || this.options.ecmaVersion >= 6 && this.isContextual("of")) && init$1.declarations.length === 1) {
      if (this.options.ecmaVersion >= 9) {
        if (this.type === types$1._in) {
          if (awaitAt > -1) {
            this.unexpected(awaitAt);
          }
        } else {
          node.await = awaitAt > -1;
        }
      }
      return this.parseForIn(node, init$1);
    }
    if (awaitAt > -1) {
      this.unexpected(awaitAt);
    }
    return this.parseFor(node, init$1);
  }
  var startsWithLet = this.isContextual("let"), isForOf = false;
  var containsEsc = this.containsEsc;
  var refDestructuringErrors = new DestructuringErrors();
  var initPos = this.start;
  var init = awaitAt > -1 ? this.parseExprSubscripts(refDestructuringErrors, "await") : this.parseExpression(true, refDestructuringErrors);
  if (this.type === types$1._in || (isForOf = this.options.ecmaVersion >= 6 && this.isContextual("of"))) {
    if (awaitAt > -1) {
      if (this.type === types$1._in) {
        this.unexpected(awaitAt);
      }
      node.await = true;
    } else if (isForOf && this.options.ecmaVersion >= 8) {
      if (init.start === initPos && !containsEsc && init.type === "Identifier" && init.name === "async") {
        this.unexpected();
      } else if (this.options.ecmaVersion >= 9) {
        node.await = false;
      }
    }
    if (startsWithLet && isForOf) {
      this.raise(init.start, "The left-hand side of a for-of loop may not start with 'let'.");
    }
    this.toAssignable(init, false, refDestructuringErrors);
    this.checkLValPattern(init);
    return this.parseForIn(node, init);
  } else {
    this.checkExpressionErrors(refDestructuringErrors, true);
  }
  if (awaitAt > -1) {
    this.unexpected(awaitAt);
  }
  return this.parseFor(node, init);
};
pp$8.parseFunctionStatement = function(node, isAsync, declarationPosition) {
  this.next();
  return this.parseFunction(node, FUNC_STATEMENT | (declarationPosition ? 0 : FUNC_HANGING_STATEMENT), false, isAsync);
};
pp$8.parseIfStatement = function(node) {
  this.next();
  node.test = this.parseParenExpression();
  node.consequent = this.parseStatement("if");
  node.alternate = this.eat(types$1._else) ? this.parseStatement("if") : null;
  return this.finishNode(node, "IfStatement");
};
pp$8.parseReturnStatement = function(node) {
  if (!this.inFunction && !this.options.allowReturnOutsideFunction) {
    this.raise(this.start, "'return' outside of function");
  }
  this.next();
  if (this.eat(types$1.semi) || this.insertSemicolon()) {
    node.argument = null;
  } else {
    node.argument = this.parseExpression();
    this.semicolon();
  }
  return this.finishNode(node, "ReturnStatement");
};
pp$8.parseSwitchStatement = function(node) {
  this.next();
  node.discriminant = this.parseParenExpression();
  node.cases = [];
  this.expect(types$1.braceL);
  this.labels.push(switchLabel);
  this.enterScope(0);
  var cur;
  for (var sawDefault = false; this.type !== types$1.braceR; ) {
    if (this.type === types$1._case || this.type === types$1._default) {
      var isCase = this.type === types$1._case;
      if (cur) {
        this.finishNode(cur, "SwitchCase");
      }
      node.cases.push(cur = this.startNode());
      cur.consequent = [];
      this.next();
      if (isCase) {
        cur.test = this.parseExpression();
      } else {
        if (sawDefault) {
          this.raiseRecoverable(this.lastTokStart, "Multiple default clauses");
        }
        sawDefault = true;
        cur.test = null;
      }
      this.expect(types$1.colon);
    } else {
      if (!cur) {
        this.unexpected();
      }
      cur.consequent.push(this.parseStatement(null));
    }
  }
  this.exitScope();
  if (cur) {
    this.finishNode(cur, "SwitchCase");
  }
  this.next();
  this.labels.pop();
  return this.finishNode(node, "SwitchStatement");
};
pp$8.parseThrowStatement = function(node) {
  this.next();
  if (lineBreak.test(this.input.slice(this.lastTokEnd, this.start))) {
    this.raise(this.lastTokEnd, "Illegal newline after throw");
  }
  node.argument = this.parseExpression();
  this.semicolon();
  return this.finishNode(node, "ThrowStatement");
};
var empty$1 = [];
pp$8.parseCatchClauseParam = function() {
  var param = this.parseBindingAtom();
  var simple = param.type === "Identifier";
  this.enterScope(simple ? SCOPE_SIMPLE_CATCH : 0);
  this.checkLValPattern(param, simple ? BIND_SIMPLE_CATCH : BIND_LEXICAL);
  this.expect(types$1.parenR);
  return param;
};
pp$8.parseTryStatement = function(node) {
  this.next();
  node.block = this.parseBlock();
  node.handler = null;
  if (this.type === types$1._catch) {
    var clause = this.startNode();
    this.next();
    if (this.eat(types$1.parenL)) {
      clause.param = this.parseCatchClauseParam();
    } else {
      if (this.options.ecmaVersion < 10) {
        this.unexpected();
      }
      clause.param = null;
      this.enterScope(0);
    }
    clause.body = this.parseBlock(false);
    this.exitScope();
    node.handler = this.finishNode(clause, "CatchClause");
  }
  node.finalizer = this.eat(types$1._finally) ? this.parseBlock() : null;
  if (!node.handler && !node.finalizer) {
    this.raise(node.start, "Missing catch or finally clause");
  }
  return this.finishNode(node, "TryStatement");
};
pp$8.parseVarStatement = function(node, kind, allowMissingInitializer) {
  this.next();
  this.parseVar(node, false, kind, allowMissingInitializer);
  this.semicolon();
  return this.finishNode(node, "VariableDeclaration");
};
pp$8.parseWhileStatement = function(node) {
  this.next();
  node.test = this.parseParenExpression();
  this.labels.push(loopLabel);
  node.body = this.parseStatement("while");
  this.labels.pop();
  return this.finishNode(node, "WhileStatement");
};
pp$8.parseWithStatement = function(node) {
  if (this.strict) {
    this.raise(this.start, "'with' in strict mode");
  }
  this.next();
  node.object = this.parseParenExpression();
  node.body = this.parseStatement("with");
  return this.finishNode(node, "WithStatement");
};
pp$8.parseEmptyStatement = function(node) {
  this.next();
  return this.finishNode(node, "EmptyStatement");
};
pp$8.parseLabeledStatement = function(node, maybeName, expr, context) {
  for (var i$1 = 0, list = this.labels; i$1 < list.length; i$1 += 1) {
    var label = list[i$1];
    if (label.name === maybeName) {
      this.raise(expr.start, "Label '" + maybeName + "' is already declared");
    }
  }
  var kind = this.type.isLoop ? "loop" : this.type === types$1._switch ? "switch" : null;
  for (var i = this.labels.length - 1; i >= 0; i--) {
    var label$1 = this.labels[i];
    if (label$1.statementStart === node.start) {
      label$1.statementStart = this.start;
      label$1.kind = kind;
    } else {
      break;
    }
  }
  this.labels.push({ name: maybeName, kind, statementStart: this.start });
  node.body = this.parseStatement(context ? context.indexOf("label") === -1 ? context + "label" : context : "label");
  this.labels.pop();
  node.label = expr;
  return this.finishNode(node, "LabeledStatement");
};
pp$8.parseExpressionStatement = function(node, expr) {
  node.expression = expr;
  this.semicolon();
  return this.finishNode(node, "ExpressionStatement");
};
pp$8.parseBlock = function(createNewLexicalScope, node, exitStrict) {
  if (createNewLexicalScope === void 0) createNewLexicalScope = true;
  if (node === void 0) node = this.startNode();
  node.body = [];
  this.expect(types$1.braceL);
  if (createNewLexicalScope) {
    this.enterScope(0);
  }
  while (this.type !== types$1.braceR) {
    var stmt = this.parseStatement(null);
    node.body.push(stmt);
  }
  if (exitStrict) {
    this.strict = false;
  }
  this.next();
  if (createNewLexicalScope) {
    this.exitScope();
  }
  return this.finishNode(node, "BlockStatement");
};
pp$8.parseFor = function(node, init) {
  node.init = init;
  this.expect(types$1.semi);
  node.test = this.type === types$1.semi ? null : this.parseExpression();
  this.expect(types$1.semi);
  node.update = this.type === types$1.parenR ? null : this.parseExpression();
  this.expect(types$1.parenR);
  node.body = this.parseStatement("for");
  this.exitScope();
  this.labels.pop();
  return this.finishNode(node, "ForStatement");
};
pp$8.parseForIn = function(node, init) {
  var isForIn = this.type === types$1._in;
  this.next();
  if (init.type === "VariableDeclaration" && init.declarations[0].init != null && (!isForIn || this.options.ecmaVersion < 8 || this.strict || init.kind !== "var" || init.declarations[0].id.type !== "Identifier")) {
    this.raise(
      init.start,
      (isForIn ? "for-in" : "for-of") + " loop variable declaration may not have an initializer"
    );
  }
  node.left = init;
  node.right = isForIn ? this.parseExpression() : this.parseMaybeAssign();
  this.expect(types$1.parenR);
  node.body = this.parseStatement("for");
  this.exitScope();
  this.labels.pop();
  return this.finishNode(node, isForIn ? "ForInStatement" : "ForOfStatement");
};
pp$8.parseVar = function(node, isFor, kind, allowMissingInitializer) {
  node.declarations = [];
  node.kind = kind;
  for (; ; ) {
    var decl = this.startNode();
    this.parseVarId(decl, kind);
    if (this.eat(types$1.eq)) {
      decl.init = this.parseMaybeAssign(isFor);
    } else if (!allowMissingInitializer && kind === "const" && !(this.type === types$1._in || this.options.ecmaVersion >= 6 && this.isContextual("of"))) {
      this.unexpected();
    } else if (!allowMissingInitializer && decl.id.type !== "Identifier" && !(isFor && (this.type === types$1._in || this.isContextual("of")))) {
      this.raise(this.lastTokEnd, "Complex binding patterns require an initialization value");
    } else {
      decl.init = null;
    }
    node.declarations.push(this.finishNode(decl, "VariableDeclarator"));
    if (!this.eat(types$1.comma)) {
      break;
    }
  }
  return node;
};
pp$8.parseVarId = function(decl, kind) {
  decl.id = this.parseBindingAtom();
  this.checkLValPattern(decl.id, kind === "var" ? BIND_VAR : BIND_LEXICAL, false);
};
var FUNC_STATEMENT = 1;
var FUNC_HANGING_STATEMENT = 2;
var FUNC_NULLABLE_ID = 4;
pp$8.parseFunction = function(node, statement, allowExpressionBody, isAsync, forInit) {
  this.initFunction(node);
  if (this.options.ecmaVersion >= 9 || this.options.ecmaVersion >= 6 && !isAsync) {
    if (this.type === types$1.star && statement & FUNC_HANGING_STATEMENT) {
      this.unexpected();
    }
    node.generator = this.eat(types$1.star);
  }
  if (this.options.ecmaVersion >= 8) {
    node.async = !!isAsync;
  }
  if (statement & FUNC_STATEMENT) {
    node.id = statement & FUNC_NULLABLE_ID && this.type !== types$1.name ? null : this.parseIdent();
    if (node.id && !(statement & FUNC_HANGING_STATEMENT)) {
      this.checkLValSimple(node.id, this.strict || node.generator || node.async ? this.treatFunctionsAsVar ? BIND_VAR : BIND_LEXICAL : BIND_FUNCTION);
    }
  }
  var oldYieldPos = this.yieldPos, oldAwaitPos = this.awaitPos, oldAwaitIdentPos = this.awaitIdentPos;
  this.yieldPos = 0;
  this.awaitPos = 0;
  this.awaitIdentPos = 0;
  this.enterScope(functionFlags(node.async, node.generator));
  if (!(statement & FUNC_STATEMENT)) {
    node.id = this.type === types$1.name ? this.parseIdent() : null;
  }
  this.parseFunctionParams(node);
  this.parseFunctionBody(node, allowExpressionBody, false, forInit);
  this.yieldPos = oldYieldPos;
  this.awaitPos = oldAwaitPos;
  this.awaitIdentPos = oldAwaitIdentPos;
  return this.finishNode(node, statement & FUNC_STATEMENT ? "FunctionDeclaration" : "FunctionExpression");
};
pp$8.parseFunctionParams = function(node) {
  this.expect(types$1.parenL);
  node.params = this.parseBindingList(types$1.parenR, false, this.options.ecmaVersion >= 8);
  this.checkYieldAwaitInDefaultParams();
};
pp$8.parseClass = function(node, isStatement) {
  this.next();
  var oldStrict = this.strict;
  this.strict = true;
  this.parseClassId(node, isStatement);
  this.parseClassSuper(node);
  var privateNameMap = this.enterClassBody();
  var classBody = this.startNode();
  var hadConstructor = false;
  classBody.body = [];
  this.expect(types$1.braceL);
  while (this.type !== types$1.braceR) {
    var element = this.parseClassElement(node.superClass !== null);
    if (element) {
      classBody.body.push(element);
      if (element.type === "MethodDefinition" && element.kind === "constructor") {
        if (hadConstructor) {
          this.raiseRecoverable(element.start, "Duplicate constructor in the same class");
        }
        hadConstructor = true;
      } else if (element.key && element.key.type === "PrivateIdentifier" && isPrivateNameConflicted(privateNameMap, element)) {
        this.raiseRecoverable(element.key.start, "Identifier '#" + element.key.name + "' has already been declared");
      }
    }
  }
  this.strict = oldStrict;
  this.next();
  node.body = this.finishNode(classBody, "ClassBody");
  this.exitClassBody();
  return this.finishNode(node, isStatement ? "ClassDeclaration" : "ClassExpression");
};
pp$8.parseClassElement = function(constructorAllowsSuper) {
  if (this.eat(types$1.semi)) {
    return null;
  }
  var ecmaVersion = this.options.ecmaVersion;
  var node = this.startNode();
  var keyName = "";
  var isGenerator = false;
  var isAsync = false;
  var kind = "method";
  var isStatic = false;
  if (this.eatContextual("static")) {
    if (ecmaVersion >= 13 && this.eat(types$1.braceL)) {
      this.parseClassStaticBlock(node);
      return node;
    }
    if (this.isClassElementNameStart() || this.type === types$1.star) {
      isStatic = true;
    } else {
      keyName = "static";
    }
  }
  node.static = isStatic;
  if (!keyName && ecmaVersion >= 8 && this.eatContextual("async")) {
    if ((this.isClassElementNameStart() || this.type === types$1.star) && !this.canInsertSemicolon()) {
      isAsync = true;
    } else {
      keyName = "async";
    }
  }
  if (!keyName && (ecmaVersion >= 9 || !isAsync) && this.eat(types$1.star)) {
    isGenerator = true;
  }
  if (!keyName && !isAsync && !isGenerator) {
    var lastValue = this.value;
    if (this.eatContextual("get") || this.eatContextual("set")) {
      if (this.isClassElementNameStart()) {
        kind = lastValue;
      } else {
        keyName = lastValue;
      }
    }
  }
  if (keyName) {
    node.computed = false;
    node.key = this.startNodeAt(this.lastTokStart, this.lastTokStartLoc);
    node.key.name = keyName;
    this.finishNode(node.key, "Identifier");
  } else {
    this.parseClassElementName(node);
  }
  if (ecmaVersion < 13 || this.type === types$1.parenL || kind !== "method" || isGenerator || isAsync) {
    var isConstructor = !node.static && checkKeyName(node, "constructor");
    var allowsDirectSuper = isConstructor && constructorAllowsSuper;
    if (isConstructor && kind !== "method") {
      this.raise(node.key.start, "Constructor can't have get/set modifier");
    }
    node.kind = isConstructor ? "constructor" : kind;
    this.parseClassMethod(node, isGenerator, isAsync, allowsDirectSuper);
  } else {
    this.parseClassField(node);
  }
  return node;
};
pp$8.isClassElementNameStart = function() {
  return this.type === types$1.name || this.type === types$1.privateId || this.type === types$1.num || this.type === types$1.string || this.type === types$1.bracketL || this.type.keyword;
};
pp$8.parseClassElementName = function(element) {
  if (this.type === types$1.privateId) {
    if (this.value === "constructor") {
      this.raise(this.start, "Classes can't have an element named '#constructor'");
    }
    element.computed = false;
    element.key = this.parsePrivateIdent();
  } else {
    this.parsePropertyName(element);
  }
};
pp$8.parseClassMethod = function(method, isGenerator, isAsync, allowsDirectSuper) {
  var key = method.key;
  if (method.kind === "constructor") {
    if (isGenerator) {
      this.raise(key.start, "Constructor can't be a generator");
    }
    if (isAsync) {
      this.raise(key.start, "Constructor can't be an async method");
    }
  } else if (method.static && checkKeyName(method, "prototype")) {
    this.raise(key.start, "Classes may not have a static property named prototype");
  }
  var value = method.value = this.parseMethod(isGenerator, isAsync, allowsDirectSuper);
  if (method.kind === "get" && value.params.length !== 0) {
    this.raiseRecoverable(value.start, "getter should have no params");
  }
  if (method.kind === "set" && value.params.length !== 1) {
    this.raiseRecoverable(value.start, "setter should have exactly one param");
  }
  if (method.kind === "set" && value.params[0].type === "RestElement") {
    this.raiseRecoverable(value.params[0].start, "Setter cannot use rest params");
  }
  return this.finishNode(method, "MethodDefinition");
};
pp$8.parseClassField = function(field) {
  if (checkKeyName(field, "constructor")) {
    this.raise(field.key.start, "Classes can't have a field named 'constructor'");
  } else if (field.static && checkKeyName(field, "prototype")) {
    this.raise(field.key.start, "Classes can't have a static field named 'prototype'");
  }
  if (this.eat(types$1.eq)) {
    var scope = this.currentThisScope();
    var inClassFieldInit = scope.inClassFieldInit;
    scope.inClassFieldInit = true;
    field.value = this.parseMaybeAssign();
    scope.inClassFieldInit = inClassFieldInit;
  } else {
    field.value = null;
  }
  this.semicolon();
  return this.finishNode(field, "PropertyDefinition");
};
pp$8.parseClassStaticBlock = function(node) {
  node.body = [];
  var oldLabels = this.labels;
  this.labels = [];
  this.enterScope(SCOPE_CLASS_STATIC_BLOCK | SCOPE_SUPER);
  while (this.type !== types$1.braceR) {
    var stmt = this.parseStatement(null);
    node.body.push(stmt);
  }
  this.next();
  this.exitScope();
  this.labels = oldLabels;
  return this.finishNode(node, "StaticBlock");
};
pp$8.parseClassId = function(node, isStatement) {
  if (this.type === types$1.name) {
    node.id = this.parseIdent();
    if (isStatement) {
      this.checkLValSimple(node.id, BIND_LEXICAL, false);
    }
  } else {
    if (isStatement === true) {
      this.unexpected();
    }
    node.id = null;
  }
};
pp$8.parseClassSuper = function(node) {
  node.superClass = this.eat(types$1._extends) ? this.parseExprSubscripts(null, false) : null;
};
pp$8.enterClassBody = function() {
  var element = { declared: /* @__PURE__ */ Object.create(null), used: [] };
  this.privateNameStack.push(element);
  return element.declared;
};
pp$8.exitClassBody = function() {
  var ref2 = this.privateNameStack.pop();
  var declared = ref2.declared;
  var used = ref2.used;
  if (!this.options.checkPrivateFields) {
    return;
  }
  var len = this.privateNameStack.length;
  var parent = len === 0 ? null : this.privateNameStack[len - 1];
  for (var i = 0; i < used.length; ++i) {
    var id = used[i];
    if (!hasOwn(declared, id.name)) {
      if (parent) {
        parent.used.push(id);
      } else {
        this.raiseRecoverable(id.start, "Private field '#" + id.name + "' must be declared in an enclosing class");
      }
    }
  }
};
function isPrivateNameConflicted(privateNameMap, element) {
  var name = element.key.name;
  var curr = privateNameMap[name];
  var next = "true";
  if (element.type === "MethodDefinition" && (element.kind === "get" || element.kind === "set")) {
    next = (element.static ? "s" : "i") + element.kind;
  }
  if (curr === "iget" && next === "iset" || curr === "iset" && next === "iget" || curr === "sget" && next === "sset" || curr === "sset" && next === "sget") {
    privateNameMap[name] = "true";
    return false;
  } else if (!curr) {
    privateNameMap[name] = next;
    return false;
  } else {
    return true;
  }
}
function checkKeyName(node, name) {
  var computed = node.computed;
  var key = node.key;
  return !computed && (key.type === "Identifier" && key.name === name || key.type === "Literal" && key.value === name);
}
pp$8.parseExportAllDeclaration = function(node, exports) {
  if (this.options.ecmaVersion >= 11) {
    if (this.eatContextual("as")) {
      node.exported = this.parseModuleExportName();
      this.checkExport(exports, node.exported, this.lastTokStart);
    } else {
      node.exported = null;
    }
  }
  this.expectContextual("from");
  if (this.type !== types$1.string) {
    this.unexpected();
  }
  node.source = this.parseExprAtom();
  if (this.options.ecmaVersion >= 16) {
    node.attributes = this.parseWithClause();
  }
  this.semicolon();
  return this.finishNode(node, "ExportAllDeclaration");
};
pp$8.parseExport = function(node, exports) {
  this.next();
  if (this.eat(types$1.star)) {
    return this.parseExportAllDeclaration(node, exports);
  }
  if (this.eat(types$1._default)) {
    this.checkExport(exports, "default", this.lastTokStart);
    node.declaration = this.parseExportDefaultDeclaration();
    return this.finishNode(node, "ExportDefaultDeclaration");
  }
  if (this.shouldParseExportStatement()) {
    node.declaration = this.parseExportDeclaration(node);
    if (node.declaration.type === "VariableDeclaration") {
      this.checkVariableExport(exports, node.declaration.declarations);
    } else {
      this.checkExport(exports, node.declaration.id, node.declaration.id.start);
    }
    node.specifiers = [];
    node.source = null;
  } else {
    node.declaration = null;
    node.specifiers = this.parseExportSpecifiers(exports);
    if (this.eatContextual("from")) {
      if (this.type !== types$1.string) {
        this.unexpected();
      }
      node.source = this.parseExprAtom();
      if (this.options.ecmaVersion >= 16) {
        node.attributes = this.parseWithClause();
      }
    } else {
      for (var i = 0, list = node.specifiers; i < list.length; i += 1) {
        var spec = list[i];
        this.checkUnreserved(spec.local);
        this.checkLocalExport(spec.local);
        if (spec.local.type === "Literal") {
          this.raise(spec.local.start, "A string literal cannot be used as an exported binding without `from`.");
        }
      }
      node.source = null;
    }
    this.semicolon();
  }
  return this.finishNode(node, "ExportNamedDeclaration");
};
pp$8.parseExportDeclaration = function(node) {
  return this.parseStatement(null);
};
pp$8.parseExportDefaultDeclaration = function() {
  var isAsync;
  if (this.type === types$1._function || (isAsync = this.isAsyncFunction())) {
    var fNode = this.startNode();
    this.next();
    if (isAsync) {
      this.next();
    }
    return this.parseFunction(fNode, FUNC_STATEMENT | FUNC_NULLABLE_ID, false, isAsync);
  } else if (this.type === types$1._class) {
    var cNode = this.startNode();
    return this.parseClass(cNode, "nullableID");
  } else {
    var declaration = this.parseMaybeAssign();
    this.semicolon();
    return declaration;
  }
};
pp$8.checkExport = function(exports, name, pos) {
  if (!exports) {
    return;
  }
  if (typeof name !== "string") {
    name = name.type === "Identifier" ? name.name : name.value;
  }
  if (hasOwn(exports, name)) {
    this.raiseRecoverable(pos, "Duplicate export '" + name + "'");
  }
  exports[name] = true;
};
pp$8.checkPatternExport = function(exports, pat) {
  var type = pat.type;
  if (type === "Identifier") {
    this.checkExport(exports, pat, pat.start);
  } else if (type === "ObjectPattern") {
    for (var i = 0, list = pat.properties; i < list.length; i += 1) {
      var prop = list[i];
      this.checkPatternExport(exports, prop);
    }
  } else if (type === "ArrayPattern") {
    for (var i$1 = 0, list$1 = pat.elements; i$1 < list$1.length; i$1 += 1) {
      var elt = list$1[i$1];
      if (elt) {
        this.checkPatternExport(exports, elt);
      }
    }
  } else if (type === "Property") {
    this.checkPatternExport(exports, pat.value);
  } else if (type === "AssignmentPattern") {
    this.checkPatternExport(exports, pat.left);
  } else if (type === "RestElement") {
    this.checkPatternExport(exports, pat.argument);
  }
};
pp$8.checkVariableExport = function(exports, decls) {
  if (!exports) {
    return;
  }
  for (var i = 0, list = decls; i < list.length; i += 1) {
    var decl = list[i];
    this.checkPatternExport(exports, decl.id);
  }
};
pp$8.shouldParseExportStatement = function() {
  return this.type.keyword === "var" || this.type.keyword === "const" || this.type.keyword === "class" || this.type.keyword === "function" || this.isLet() || this.isAsyncFunction();
};
pp$8.parseExportSpecifier = function(exports) {
  var node = this.startNode();
  node.local = this.parseModuleExportName();
  node.exported = this.eatContextual("as") ? this.parseModuleExportName() : node.local;
  this.checkExport(
    exports,
    node.exported,
    node.exported.start
  );
  return this.finishNode(node, "ExportSpecifier");
};
pp$8.parseExportSpecifiers = function(exports) {
  var nodes = [], first = true;
  this.expect(types$1.braceL);
  while (!this.eat(types$1.braceR)) {
    if (!first) {
      this.expect(types$1.comma);
      if (this.afterTrailingComma(types$1.braceR)) {
        break;
      }
    } else {
      first = false;
    }
    nodes.push(this.parseExportSpecifier(exports));
  }
  return nodes;
};
pp$8.parseImport = function(node) {
  this.next();
  if (this.type === types$1.string) {
    node.specifiers = empty$1;
    node.source = this.parseExprAtom();
  } else {
    node.specifiers = this.parseImportSpecifiers();
    this.expectContextual("from");
    node.source = this.type === types$1.string ? this.parseExprAtom() : this.unexpected();
  }
  if (this.options.ecmaVersion >= 16) {
    node.attributes = this.parseWithClause();
  }
  this.semicolon();
  return this.finishNode(node, "ImportDeclaration");
};
pp$8.parseImportSpecifier = function() {
  var node = this.startNode();
  node.imported = this.parseModuleExportName();
  if (this.eatContextual("as")) {
    node.local = this.parseIdent();
  } else {
    this.checkUnreserved(node.imported);
    node.local = node.imported;
  }
  this.checkLValSimple(node.local, BIND_LEXICAL);
  return this.finishNode(node, "ImportSpecifier");
};
pp$8.parseImportDefaultSpecifier = function() {
  var node = this.startNode();
  node.local = this.parseIdent();
  this.checkLValSimple(node.local, BIND_LEXICAL);
  return this.finishNode(node, "ImportDefaultSpecifier");
};
pp$8.parseImportNamespaceSpecifier = function() {
  var node = this.startNode();
  this.next();
  this.expectContextual("as");
  node.local = this.parseIdent();
  this.checkLValSimple(node.local, BIND_LEXICAL);
  return this.finishNode(node, "ImportNamespaceSpecifier");
};
pp$8.parseImportSpecifiers = function() {
  var nodes = [], first = true;
  if (this.type === types$1.name) {
    nodes.push(this.parseImportDefaultSpecifier());
    if (!this.eat(types$1.comma)) {
      return nodes;
    }
  }
  if (this.type === types$1.star) {
    nodes.push(this.parseImportNamespaceSpecifier());
    return nodes;
  }
  this.expect(types$1.braceL);
  while (!this.eat(types$1.braceR)) {
    if (!first) {
      this.expect(types$1.comma);
      if (this.afterTrailingComma(types$1.braceR)) {
        break;
      }
    } else {
      first = false;
    }
    nodes.push(this.parseImportSpecifier());
  }
  return nodes;
};
pp$8.parseWithClause = function() {
  var nodes = [];
  if (!this.eat(types$1._with)) {
    return nodes;
  }
  this.expect(types$1.braceL);
  var attributeKeys = {};
  var first = true;
  while (!this.eat(types$1.braceR)) {
    if (!first) {
      this.expect(types$1.comma);
      if (this.afterTrailingComma(types$1.braceR)) {
        break;
      }
    } else {
      first = false;
    }
    var attr = this.parseImportAttribute();
    var keyName = attr.key.type === "Identifier" ? attr.key.name : attr.key.value;
    if (hasOwn(attributeKeys, keyName)) {
      this.raiseRecoverable(attr.key.start, "Duplicate attribute key '" + keyName + "'");
    }
    attributeKeys[keyName] = true;
    nodes.push(attr);
  }
  return nodes;
};
pp$8.parseImportAttribute = function() {
  var node = this.startNode();
  node.key = this.type === types$1.string ? this.parseExprAtom() : this.parseIdent(this.options.allowReserved !== "never");
  this.expect(types$1.colon);
  if (this.type !== types$1.string) {
    this.unexpected();
  }
  node.value = this.parseExprAtom();
  return this.finishNode(node, "ImportAttribute");
};
pp$8.parseModuleExportName = function() {
  if (this.options.ecmaVersion >= 13 && this.type === types$1.string) {
    var stringLiteral = this.parseLiteral(this.value);
    if (loneSurrogate.test(stringLiteral.value)) {
      this.raise(stringLiteral.start, "An export name cannot include a lone surrogate.");
    }
    return stringLiteral;
  }
  return this.parseIdent(true);
};
pp$8.adaptDirectivePrologue = function(statements) {
  for (var i = 0; i < statements.length && this.isDirectiveCandidate(statements[i]); ++i) {
    statements[i].directive = statements[i].expression.raw.slice(1, -1);
  }
};
pp$8.isDirectiveCandidate = function(statement) {
  return this.options.ecmaVersion >= 5 && statement.type === "ExpressionStatement" && statement.expression.type === "Literal" && typeof statement.expression.value === "string" && // Reject parenthesized strings.
  (this.input[statement.start] === '"' || this.input[statement.start] === "'");
};
var pp$7 = Parser.prototype;
pp$7.toAssignable = function(node, isBinding, refDestructuringErrors) {
  if (this.options.ecmaVersion >= 6 && node) {
    switch (node.type) {
      case "Identifier":
        if (this.inAsync && node.name === "await") {
          this.raise(node.start, "Cannot use 'await' as identifier inside an async function");
        }
        break;
      case "ObjectPattern":
      case "ArrayPattern":
      case "AssignmentPattern":
      case "RestElement":
        break;
      case "ObjectExpression":
        node.type = "ObjectPattern";
        if (refDestructuringErrors) {
          this.checkPatternErrors(refDestructuringErrors, true);
        }
        for (var i = 0, list = node.properties; i < list.length; i += 1) {
          var prop = list[i];
          this.toAssignable(prop, isBinding);
          if (prop.type === "RestElement" && (prop.argument.type === "ArrayPattern" || prop.argument.type === "ObjectPattern")) {
            this.raise(prop.argument.start, "Unexpected token");
          }
        }
        break;
      case "Property":
        if (node.kind !== "init") {
          this.raise(node.key.start, "Object pattern can't contain getter or setter");
        }
        this.toAssignable(node.value, isBinding);
        break;
      case "ArrayExpression":
        node.type = "ArrayPattern";
        if (refDestructuringErrors) {
          this.checkPatternErrors(refDestructuringErrors, true);
        }
        this.toAssignableList(node.elements, isBinding);
        break;
      case "SpreadElement":
        node.type = "RestElement";
        this.toAssignable(node.argument, isBinding);
        if (node.argument.type === "AssignmentPattern") {
          this.raise(node.argument.start, "Rest elements cannot have a default value");
        }
        break;
      case "AssignmentExpression":
        if (node.operator !== "=") {
          this.raise(node.left.end, "Only '=' operator can be used for specifying default value.");
        }
        node.type = "AssignmentPattern";
        delete node.operator;
        this.toAssignable(node.left, isBinding);
        break;
      case "ParenthesizedExpression":
        this.toAssignable(node.expression, isBinding, refDestructuringErrors);
        break;
      case "ChainExpression":
        this.raiseRecoverable(node.start, "Optional chaining cannot appear in left-hand side");
        break;
      case "MemberExpression":
        if (!isBinding) {
          break;
        }
      default:
        this.raise(node.start, "Assigning to rvalue");
    }
  } else if (refDestructuringErrors) {
    this.checkPatternErrors(refDestructuringErrors, true);
  }
  return node;
};
pp$7.toAssignableList = function(exprList, isBinding) {
  var end = exprList.length;
  for (var i = 0; i < end; i++) {
    var elt = exprList[i];
    if (elt) {
      this.toAssignable(elt, isBinding);
    }
  }
  if (end) {
    var last = exprList[end - 1];
    if (this.options.ecmaVersion === 6 && isBinding && last && last.type === "RestElement" && last.argument.type !== "Identifier") {
      this.unexpected(last.argument.start);
    }
  }
  return exprList;
};
pp$7.parseSpread = function(refDestructuringErrors) {
  var node = this.startNode();
  this.next();
  node.argument = this.parseMaybeAssign(false, refDestructuringErrors);
  return this.finishNode(node, "SpreadElement");
};
pp$7.parseRestBinding = function() {
  var node = this.startNode();
  this.next();
  if (this.options.ecmaVersion === 6 && this.type !== types$1.name) {
    this.unexpected();
  }
  node.argument = this.parseBindingAtom();
  return this.finishNode(node, "RestElement");
};
pp$7.parseBindingAtom = function() {
  if (this.options.ecmaVersion >= 6) {
    switch (this.type) {
      case types$1.bracketL:
        var node = this.startNode();
        this.next();
        node.elements = this.parseBindingList(types$1.bracketR, true, true);
        return this.finishNode(node, "ArrayPattern");
      case types$1.braceL:
        return this.parseObj(true);
    }
  }
  return this.parseIdent();
};
pp$7.parseBindingList = function(close, allowEmpty, allowTrailingComma, allowModifiers) {
  var elts = [], first = true;
  while (!this.eat(close)) {
    if (first) {
      first = false;
    } else {
      this.expect(types$1.comma);
    }
    if (allowEmpty && this.type === types$1.comma) {
      elts.push(null);
    } else if (allowTrailingComma && this.afterTrailingComma(close)) {
      break;
    } else if (this.type === types$1.ellipsis) {
      var rest = this.parseRestBinding();
      this.parseBindingListItem(rest);
      elts.push(rest);
      if (this.type === types$1.comma) {
        this.raiseRecoverable(this.start, "Comma is not permitted after the rest element");
      }
      this.expect(close);
      break;
    } else {
      elts.push(this.parseAssignableListItem(allowModifiers));
    }
  }
  return elts;
};
pp$7.parseAssignableListItem = function(allowModifiers) {
  var elem = this.parseMaybeDefault(this.start, this.startLoc);
  this.parseBindingListItem(elem);
  return elem;
};
pp$7.parseBindingListItem = function(param) {
  return param;
};
pp$7.parseMaybeDefault = function(startPos, startLoc, left) {
  left = left || this.parseBindingAtom();
  if (this.options.ecmaVersion < 6 || !this.eat(types$1.eq)) {
    return left;
  }
  var node = this.startNodeAt(startPos, startLoc);
  node.left = left;
  node.right = this.parseMaybeAssign();
  return this.finishNode(node, "AssignmentPattern");
};
pp$7.checkLValSimple = function(expr, bindingType, checkClashes) {
  if (bindingType === void 0) bindingType = BIND_NONE;
  var isBind = bindingType !== BIND_NONE;
  switch (expr.type) {
    case "Identifier":
      if (this.strict && this.reservedWordsStrictBind.test(expr.name)) {
        this.raiseRecoverable(expr.start, (isBind ? "Binding " : "Assigning to ") + expr.name + " in strict mode");
      }
      if (isBind) {
        if (bindingType === BIND_LEXICAL && expr.name === "let") {
          this.raiseRecoverable(expr.start, "let is disallowed as a lexically bound name");
        }
        if (checkClashes) {
          if (hasOwn(checkClashes, expr.name)) {
            this.raiseRecoverable(expr.start, "Argument name clash");
          }
          checkClashes[expr.name] = true;
        }
        if (bindingType !== BIND_OUTSIDE) {
          this.declareName(expr.name, bindingType, expr.start);
        }
      }
      break;
    case "ChainExpression":
      this.raiseRecoverable(expr.start, "Optional chaining cannot appear in left-hand side");
      break;
    case "MemberExpression":
      if (isBind) {
        this.raiseRecoverable(expr.start, "Binding member expression");
      }
      break;
    case "ParenthesizedExpression":
      if (isBind) {
        this.raiseRecoverable(expr.start, "Binding parenthesized expression");
      }
      return this.checkLValSimple(expr.expression, bindingType, checkClashes);
    default:
      this.raise(expr.start, (isBind ? "Binding" : "Assigning to") + " rvalue");
  }
};
pp$7.checkLValPattern = function(expr, bindingType, checkClashes) {
  if (bindingType === void 0) bindingType = BIND_NONE;
  switch (expr.type) {
    case "ObjectPattern":
      for (var i = 0, list = expr.properties; i < list.length; i += 1) {
        var prop = list[i];
        this.checkLValInnerPattern(prop, bindingType, checkClashes);
      }
      break;
    case "ArrayPattern":
      for (var i$1 = 0, list$1 = expr.elements; i$1 < list$1.length; i$1 += 1) {
        var elem = list$1[i$1];
        if (elem) {
          this.checkLValInnerPattern(elem, bindingType, checkClashes);
        }
      }
      break;
    default:
      this.checkLValSimple(expr, bindingType, checkClashes);
  }
};
pp$7.checkLValInnerPattern = function(expr, bindingType, checkClashes) {
  if (bindingType === void 0) bindingType = BIND_NONE;
  switch (expr.type) {
    case "Property":
      this.checkLValInnerPattern(expr.value, bindingType, checkClashes);
      break;
    case "AssignmentPattern":
      this.checkLValPattern(expr.left, bindingType, checkClashes);
      break;
    case "RestElement":
      this.checkLValPattern(expr.argument, bindingType, checkClashes);
      break;
    default:
      this.checkLValPattern(expr, bindingType, checkClashes);
  }
};
var TokContext = function TokContext2(token, isExpr, preserveSpace, override, generator) {
  this.token = token;
  this.isExpr = !!isExpr;
  this.preserveSpace = !!preserveSpace;
  this.override = override;
  this.generator = !!generator;
};
var types = {
  b_stat: new TokContext("{", false),
  b_expr: new TokContext("{", true),
  b_tmpl: new TokContext("${", false),
  p_stat: new TokContext("(", false),
  p_expr: new TokContext("(", true),
  q_tmpl: new TokContext("`", true, true, function(p) {
    return p.tryReadTemplateToken();
  }),
  f_stat: new TokContext("function", false),
  f_expr: new TokContext("function", true),
  f_expr_gen: new TokContext("function", true, false, null, true),
  f_gen: new TokContext("function", false, false, null, true)
};
var pp$6 = Parser.prototype;
pp$6.initialContext = function() {
  return [types.b_stat];
};
pp$6.curContext = function() {
  return this.context[this.context.length - 1];
};
pp$6.braceIsBlock = function(prevType) {
  var parent = this.curContext();
  if (parent === types.f_expr || parent === types.f_stat) {
    return true;
  }
  if (prevType === types$1.colon && (parent === types.b_stat || parent === types.b_expr)) {
    return !parent.isExpr;
  }
  if (prevType === types$1._return || prevType === types$1.name && this.exprAllowed) {
    return lineBreak.test(this.input.slice(this.lastTokEnd, this.start));
  }
  if (prevType === types$1._else || prevType === types$1.semi || prevType === types$1.eof || prevType === types$1.parenR || prevType === types$1.arrow) {
    return true;
  }
  if (prevType === types$1.braceL) {
    return parent === types.b_stat;
  }
  if (prevType === types$1._var || prevType === types$1._const || prevType === types$1.name) {
    return false;
  }
  return !this.exprAllowed;
};
pp$6.inGeneratorContext = function() {
  for (var i = this.context.length - 1; i >= 1; i--) {
    var context = this.context[i];
    if (context.token === "function") {
      return context.generator;
    }
  }
  return false;
};
pp$6.updateContext = function(prevType) {
  var update, type = this.type;
  if (type.keyword && prevType === types$1.dot) {
    this.exprAllowed = false;
  } else if (update = type.updateContext) {
    update.call(this, prevType);
  } else {
    this.exprAllowed = type.beforeExpr;
  }
};
pp$6.overrideContext = function(tokenCtx) {
  if (this.curContext() !== tokenCtx) {
    this.context[this.context.length - 1] = tokenCtx;
  }
};
types$1.parenR.updateContext = types$1.braceR.updateContext = function() {
  if (this.context.length === 1) {
    this.exprAllowed = true;
    return;
  }
  var out2 = this.context.pop();
  if (out2 === types.b_stat && this.curContext().token === "function") {
    out2 = this.context.pop();
  }
  this.exprAllowed = !out2.isExpr;
};
types$1.braceL.updateContext = function(prevType) {
  this.context.push(this.braceIsBlock(prevType) ? types.b_stat : types.b_expr);
  this.exprAllowed = true;
};
types$1.dollarBraceL.updateContext = function() {
  this.context.push(types.b_tmpl);
  this.exprAllowed = true;
};
types$1.parenL.updateContext = function(prevType) {
  var statementParens = prevType === types$1._if || prevType === types$1._for || prevType === types$1._with || prevType === types$1._while;
  this.context.push(statementParens ? types.p_stat : types.p_expr);
  this.exprAllowed = true;
};
types$1.incDec.updateContext = function() {
};
types$1._function.updateContext = types$1._class.updateContext = function(prevType) {
  if (prevType.beforeExpr && prevType !== types$1._else && !(prevType === types$1.semi && this.curContext() !== types.p_stat) && !(prevType === types$1._return && lineBreak.test(this.input.slice(this.lastTokEnd, this.start))) && !((prevType === types$1.colon || prevType === types$1.braceL) && this.curContext() === types.b_stat)) {
    this.context.push(types.f_expr);
  } else {
    this.context.push(types.f_stat);
  }
  this.exprAllowed = false;
};
types$1.colon.updateContext = function() {
  if (this.curContext().token === "function") {
    this.context.pop();
  }
  this.exprAllowed = true;
};
types$1.backQuote.updateContext = function() {
  if (this.curContext() === types.q_tmpl) {
    this.context.pop();
  } else {
    this.context.push(types.q_tmpl);
  }
  this.exprAllowed = false;
};
types$1.star.updateContext = function(prevType) {
  if (prevType === types$1._function) {
    var index = this.context.length - 1;
    if (this.context[index] === types.f_expr) {
      this.context[index] = types.f_expr_gen;
    } else {
      this.context[index] = types.f_gen;
    }
  }
  this.exprAllowed = true;
};
types$1.name.updateContext = function(prevType) {
  var allowed = false;
  if (this.options.ecmaVersion >= 6 && prevType !== types$1.dot) {
    if (this.value === "of" && !this.exprAllowed || this.value === "yield" && this.inGeneratorContext()) {
      allowed = true;
    }
  }
  this.exprAllowed = allowed;
};
var pp$5 = Parser.prototype;
pp$5.checkPropClash = function(prop, propHash, refDestructuringErrors) {
  if (this.options.ecmaVersion >= 9 && prop.type === "SpreadElement") {
    return;
  }
  if (this.options.ecmaVersion >= 6 && (prop.computed || prop.method || prop.shorthand)) {
    return;
  }
  var key = prop.key;
  var name;
  switch (key.type) {
    case "Identifier":
      name = key.name;
      break;
    case "Literal":
      name = String(key.value);
      break;
    default:
      return;
  }
  var kind = prop.kind;
  if (this.options.ecmaVersion >= 6) {
    if (name === "__proto__" && kind === "init") {
      if (propHash.proto) {
        if (refDestructuringErrors) {
          if (refDestructuringErrors.doubleProto < 0) {
            refDestructuringErrors.doubleProto = key.start;
          }
        } else {
          this.raiseRecoverable(key.start, "Redefinition of __proto__ property");
        }
      }
      propHash.proto = true;
    }
    return;
  }
  name = "$" + name;
  var other = propHash[name];
  if (other) {
    var redefinition;
    if (kind === "init") {
      redefinition = this.strict && other.init || other.get || other.set;
    } else {
      redefinition = other.init || other[kind];
    }
    if (redefinition) {
      this.raiseRecoverable(key.start, "Redefinition of property");
    }
  } else {
    other = propHash[name] = {
      init: false,
      get: false,
      set: false
    };
  }
  other[kind] = true;
};
pp$5.parseExpression = function(forInit, refDestructuringErrors) {
  var startPos = this.start, startLoc = this.startLoc;
  var expr = this.parseMaybeAssign(forInit, refDestructuringErrors);
  if (this.type === types$1.comma) {
    var node = this.startNodeAt(startPos, startLoc);
    node.expressions = [expr];
    while (this.eat(types$1.comma)) {
      node.expressions.push(this.parseMaybeAssign(forInit, refDestructuringErrors));
    }
    return this.finishNode(node, "SequenceExpression");
  }
  return expr;
};
pp$5.parseMaybeAssign = function(forInit, refDestructuringErrors, afterLeftParse) {
  if (this.isContextual("yield")) {
    if (this.inGenerator) {
      return this.parseYield(forInit);
    } else {
      this.exprAllowed = false;
    }
  }
  var ownDestructuringErrors = false, oldParenAssign = -1, oldTrailingComma = -1, oldDoubleProto = -1;
  if (refDestructuringErrors) {
    oldParenAssign = refDestructuringErrors.parenthesizedAssign;
    oldTrailingComma = refDestructuringErrors.trailingComma;
    oldDoubleProto = refDestructuringErrors.doubleProto;
    refDestructuringErrors.parenthesizedAssign = refDestructuringErrors.trailingComma = -1;
  } else {
    refDestructuringErrors = new DestructuringErrors();
    ownDestructuringErrors = true;
  }
  var startPos = this.start, startLoc = this.startLoc;
  if (this.type === types$1.parenL || this.type === types$1.name) {
    this.potentialArrowAt = this.start;
    this.potentialArrowInForAwait = forInit === "await";
  }
  var left = this.parseMaybeConditional(forInit, refDestructuringErrors);
  if (afterLeftParse) {
    left = afterLeftParse.call(this, left, startPos, startLoc);
  }
  if (this.type.isAssign) {
    var node = this.startNodeAt(startPos, startLoc);
    node.operator = this.value;
    if (this.type === types$1.eq) {
      left = this.toAssignable(left, false, refDestructuringErrors);
    }
    if (!ownDestructuringErrors) {
      refDestructuringErrors.parenthesizedAssign = refDestructuringErrors.trailingComma = refDestructuringErrors.doubleProto = -1;
    }
    if (refDestructuringErrors.shorthandAssign >= left.start) {
      refDestructuringErrors.shorthandAssign = -1;
    }
    if (this.type === types$1.eq) {
      this.checkLValPattern(left);
    } else {
      this.checkLValSimple(left);
    }
    node.left = left;
    this.next();
    node.right = this.parseMaybeAssign(forInit);
    if (oldDoubleProto > -1) {
      refDestructuringErrors.doubleProto = oldDoubleProto;
    }
    return this.finishNode(node, "AssignmentExpression");
  } else {
    if (ownDestructuringErrors) {
      this.checkExpressionErrors(refDestructuringErrors, true);
    }
  }
  if (oldParenAssign > -1) {
    refDestructuringErrors.parenthesizedAssign = oldParenAssign;
  }
  if (oldTrailingComma > -1) {
    refDestructuringErrors.trailingComma = oldTrailingComma;
  }
  return left;
};
pp$5.parseMaybeConditional = function(forInit, refDestructuringErrors) {
  var startPos = this.start, startLoc = this.startLoc;
  var expr = this.parseExprOps(forInit, refDestructuringErrors);
  if (this.checkExpressionErrors(refDestructuringErrors)) {
    return expr;
  }
  if (this.eat(types$1.question)) {
    var node = this.startNodeAt(startPos, startLoc);
    node.test = expr;
    node.consequent = this.parseMaybeAssign();
    this.expect(types$1.colon);
    node.alternate = this.parseMaybeAssign(forInit);
    return this.finishNode(node, "ConditionalExpression");
  }
  return expr;
};
pp$5.parseExprOps = function(forInit, refDestructuringErrors) {
  var startPos = this.start, startLoc = this.startLoc;
  var expr = this.parseMaybeUnary(refDestructuringErrors, false, false, forInit);
  if (this.checkExpressionErrors(refDestructuringErrors)) {
    return expr;
  }
  return expr.start === startPos && expr.type === "ArrowFunctionExpression" ? expr : this.parseExprOp(expr, startPos, startLoc, -1, forInit);
};
pp$5.parseExprOp = function(left, leftStartPos, leftStartLoc, minPrec, forInit) {
  var prec = this.type.binop;
  if (prec != null && (!forInit || this.type !== types$1._in)) {
    if (prec > minPrec) {
      var logical = this.type === types$1.logicalOR || this.type === types$1.logicalAND;
      var coalesce = this.type === types$1.coalesce;
      if (coalesce) {
        prec = types$1.logicalAND.binop;
      }
      var op = this.value;
      this.next();
      var startPos = this.start, startLoc = this.startLoc;
      var right = this.parseExprOp(this.parseMaybeUnary(null, false, false, forInit), startPos, startLoc, prec, forInit);
      var node = this.buildBinary(leftStartPos, leftStartLoc, left, right, op, logical || coalesce);
      if (logical && this.type === types$1.coalesce || coalesce && (this.type === types$1.logicalOR || this.type === types$1.logicalAND)) {
        this.raiseRecoverable(this.start, "Logical expressions and coalesce expressions cannot be mixed. Wrap either by parentheses");
      }
      return this.parseExprOp(node, leftStartPos, leftStartLoc, minPrec, forInit);
    }
  }
  return left;
};
pp$5.buildBinary = function(startPos, startLoc, left, right, op, logical) {
  if (right.type === "PrivateIdentifier") {
    this.raise(right.start, "Private identifier can only be left side of binary expression");
  }
  var node = this.startNodeAt(startPos, startLoc);
  node.left = left;
  node.operator = op;
  node.right = right;
  return this.finishNode(node, logical ? "LogicalExpression" : "BinaryExpression");
};
pp$5.parseMaybeUnary = function(refDestructuringErrors, sawUnary, incDec, forInit) {
  var startPos = this.start, startLoc = this.startLoc, expr;
  if (this.isContextual("await") && this.canAwait) {
    expr = this.parseAwait(forInit);
    sawUnary = true;
  } else if (this.type.prefix) {
    var node = this.startNode(), update = this.type === types$1.incDec;
    node.operator = this.value;
    node.prefix = true;
    this.next();
    node.argument = this.parseMaybeUnary(null, true, update, forInit);
    this.checkExpressionErrors(refDestructuringErrors, true);
    if (update) {
      this.checkLValSimple(node.argument);
    } else if (this.strict && node.operator === "delete" && isLocalVariableAccess(node.argument)) {
      this.raiseRecoverable(node.start, "Deleting local variable in strict mode");
    } else if (node.operator === "delete" && isPrivateFieldAccess(node.argument)) {
      this.raiseRecoverable(node.start, "Private fields can not be deleted");
    } else {
      sawUnary = true;
    }
    expr = this.finishNode(node, update ? "UpdateExpression" : "UnaryExpression");
  } else if (!sawUnary && this.type === types$1.privateId) {
    if ((forInit || this.privateNameStack.length === 0) && this.options.checkPrivateFields) {
      this.unexpected();
    }
    expr = this.parsePrivateIdent();
    if (this.type !== types$1._in) {
      this.unexpected();
    }
  } else {
    expr = this.parseExprSubscripts(refDestructuringErrors, forInit);
    if (this.checkExpressionErrors(refDestructuringErrors)) {
      return expr;
    }
    while (this.type.postfix && !this.canInsertSemicolon()) {
      var node$1 = this.startNodeAt(startPos, startLoc);
      node$1.operator = this.value;
      node$1.prefix = false;
      node$1.argument = expr;
      this.checkLValSimple(expr);
      this.next();
      expr = this.finishNode(node$1, "UpdateExpression");
    }
  }
  if (!incDec && this.eat(types$1.starstar)) {
    if (sawUnary) {
      this.unexpected(this.lastTokStart);
    } else {
      return this.buildBinary(startPos, startLoc, expr, this.parseMaybeUnary(null, false, false, forInit), "**", false);
    }
  } else {
    return expr;
  }
};
function isLocalVariableAccess(node) {
  return node.type === "Identifier" || node.type === "ParenthesizedExpression" && isLocalVariableAccess(node.expression);
}
function isPrivateFieldAccess(node) {
  return node.type === "MemberExpression" && node.property.type === "PrivateIdentifier" || node.type === "ChainExpression" && isPrivateFieldAccess(node.expression) || node.type === "ParenthesizedExpression" && isPrivateFieldAccess(node.expression);
}
pp$5.parseExprSubscripts = function(refDestructuringErrors, forInit) {
  var startPos = this.start, startLoc = this.startLoc;
  var expr = this.parseExprAtom(refDestructuringErrors, forInit);
  if (expr.type === "ArrowFunctionExpression" && this.input.slice(this.lastTokStart, this.lastTokEnd) !== ")") {
    return expr;
  }
  var result = this.parseSubscripts(expr, startPos, startLoc, false, forInit);
  if (refDestructuringErrors && result.type === "MemberExpression") {
    if (refDestructuringErrors.parenthesizedAssign >= result.start) {
      refDestructuringErrors.parenthesizedAssign = -1;
    }
    if (refDestructuringErrors.parenthesizedBind >= result.start) {
      refDestructuringErrors.parenthesizedBind = -1;
    }
    if (refDestructuringErrors.trailingComma >= result.start) {
      refDestructuringErrors.trailingComma = -1;
    }
  }
  return result;
};
pp$5.parseSubscripts = function(base, startPos, startLoc, noCalls, forInit) {
  var maybeAsyncArrow = this.options.ecmaVersion >= 8 && base.type === "Identifier" && base.name === "async" && this.lastTokEnd === base.end && !this.canInsertSemicolon() && base.end - base.start === 5 && this.potentialArrowAt === base.start;
  var optionalChained = false;
  while (true) {
    var element = this.parseSubscript(base, startPos, startLoc, noCalls, maybeAsyncArrow, optionalChained, forInit);
    if (element.optional) {
      optionalChained = true;
    }
    if (element === base || element.type === "ArrowFunctionExpression") {
      if (optionalChained) {
        var chainNode = this.startNodeAt(startPos, startLoc);
        chainNode.expression = element;
        element = this.finishNode(chainNode, "ChainExpression");
      }
      return element;
    }
    base = element;
  }
};
pp$5.shouldParseAsyncArrow = function() {
  return !this.canInsertSemicolon() && this.eat(types$1.arrow);
};
pp$5.parseSubscriptAsyncArrow = function(startPos, startLoc, exprList, forInit) {
  return this.parseArrowExpression(this.startNodeAt(startPos, startLoc), exprList, true, forInit);
};
pp$5.parseSubscript = function(base, startPos, startLoc, noCalls, maybeAsyncArrow, optionalChained, forInit) {
  var optionalSupported = this.options.ecmaVersion >= 11;
  var optional = optionalSupported && this.eat(types$1.questionDot);
  if (noCalls && optional) {
    this.raise(this.lastTokStart, "Optional chaining cannot appear in the callee of new expressions");
  }
  var computed = this.eat(types$1.bracketL);
  if (computed || optional && this.type !== types$1.parenL && this.type !== types$1.backQuote || this.eat(types$1.dot)) {
    var node = this.startNodeAt(startPos, startLoc);
    node.object = base;
    if (computed) {
      node.property = this.parseExpression();
      this.expect(types$1.bracketR);
    } else if (this.type === types$1.privateId && base.type !== "Super") {
      node.property = this.parsePrivateIdent();
    } else {
      node.property = this.parseIdent(this.options.allowReserved !== "never");
    }
    node.computed = !!computed;
    if (optionalSupported) {
      node.optional = optional;
    }
    base = this.finishNode(node, "MemberExpression");
  } else if (!noCalls && this.eat(types$1.parenL)) {
    var refDestructuringErrors = new DestructuringErrors(), oldYieldPos = this.yieldPos, oldAwaitPos = this.awaitPos, oldAwaitIdentPos = this.awaitIdentPos;
    this.yieldPos = 0;
    this.awaitPos = 0;
    this.awaitIdentPos = 0;
    var exprList = this.parseExprList(types$1.parenR, this.options.ecmaVersion >= 8, false, refDestructuringErrors);
    if (maybeAsyncArrow && !optional && this.shouldParseAsyncArrow()) {
      this.checkPatternErrors(refDestructuringErrors, false);
      this.checkYieldAwaitInDefaultParams();
      if (this.awaitIdentPos > 0) {
        this.raise(this.awaitIdentPos, "Cannot use 'await' as identifier inside an async function");
      }
      this.yieldPos = oldYieldPos;
      this.awaitPos = oldAwaitPos;
      this.awaitIdentPos = oldAwaitIdentPos;
      return this.parseSubscriptAsyncArrow(startPos, startLoc, exprList, forInit);
    }
    this.checkExpressionErrors(refDestructuringErrors, true);
    this.yieldPos = oldYieldPos || this.yieldPos;
    this.awaitPos = oldAwaitPos || this.awaitPos;
    this.awaitIdentPos = oldAwaitIdentPos || this.awaitIdentPos;
    var node$1 = this.startNodeAt(startPos, startLoc);
    node$1.callee = base;
    node$1.arguments = exprList;
    if (optionalSupported) {
      node$1.optional = optional;
    }
    base = this.finishNode(node$1, "CallExpression");
  } else if (this.type === types$1.backQuote) {
    if (optional || optionalChained) {
      this.raise(this.start, "Optional chaining cannot appear in the tag of tagged template expressions");
    }
    var node$2 = this.startNodeAt(startPos, startLoc);
    node$2.tag = base;
    node$2.quasi = this.parseTemplate({ isTagged: true });
    base = this.finishNode(node$2, "TaggedTemplateExpression");
  }
  return base;
};
pp$5.parseExprAtom = function(refDestructuringErrors, forInit, forNew) {
  if (this.type === types$1.slash) {
    this.readRegexp();
  }
  var node, canBeArrow = this.potentialArrowAt === this.start;
  switch (this.type) {
    case types$1._super:
      if (!this.allowSuper) {
        this.raise(this.start, "'super' keyword outside a method");
      }
      node = this.startNode();
      this.next();
      if (this.type === types$1.parenL && !this.allowDirectSuper) {
        this.raise(node.start, "super() call outside constructor of a subclass");
      }
      if (this.type !== types$1.dot && this.type !== types$1.bracketL && this.type !== types$1.parenL) {
        this.unexpected();
      }
      return this.finishNode(node, "Super");
    case types$1._this:
      node = this.startNode();
      this.next();
      return this.finishNode(node, "ThisExpression");
    case types$1.name:
      var startPos = this.start, startLoc = this.startLoc, containsEsc = this.containsEsc;
      var id = this.parseIdent(false);
      if (this.options.ecmaVersion >= 8 && !containsEsc && id.name === "async" && !this.canInsertSemicolon() && this.eat(types$1._function)) {
        this.overrideContext(types.f_expr);
        return this.parseFunction(this.startNodeAt(startPos, startLoc), 0, false, true, forInit);
      }
      if (canBeArrow && !this.canInsertSemicolon()) {
        if (this.eat(types$1.arrow)) {
          return this.parseArrowExpression(this.startNodeAt(startPos, startLoc), [id], false, forInit);
        }
        if (this.options.ecmaVersion >= 8 && id.name === "async" && this.type === types$1.name && !containsEsc && (!this.potentialArrowInForAwait || this.value !== "of" || this.containsEsc)) {
          id = this.parseIdent(false);
          if (this.canInsertSemicolon() || !this.eat(types$1.arrow)) {
            this.unexpected();
          }
          return this.parseArrowExpression(this.startNodeAt(startPos, startLoc), [id], true, forInit);
        }
      }
      return id;
    case types$1.regexp:
      var value = this.value;
      node = this.parseLiteral(value.value);
      node.regex = { pattern: value.pattern, flags: value.flags };
      return node;
    case types$1.num:
    case types$1.string:
      return this.parseLiteral(this.value);
    case types$1._null:
    case types$1._true:
    case types$1._false:
      node = this.startNode();
      node.value = this.type === types$1._null ? null : this.type === types$1._true;
      node.raw = this.type.keyword;
      this.next();
      return this.finishNode(node, "Literal");
    case types$1.parenL:
      var start = this.start, expr = this.parseParenAndDistinguishExpression(canBeArrow, forInit);
      if (refDestructuringErrors) {
        if (refDestructuringErrors.parenthesizedAssign < 0 && !this.isSimpleAssignTarget(expr)) {
          refDestructuringErrors.parenthesizedAssign = start;
        }
        if (refDestructuringErrors.parenthesizedBind < 0) {
          refDestructuringErrors.parenthesizedBind = start;
        }
      }
      return expr;
    case types$1.bracketL:
      node = this.startNode();
      this.next();
      node.elements = this.parseExprList(types$1.bracketR, true, true, refDestructuringErrors);
      return this.finishNode(node, "ArrayExpression");
    case types$1.braceL:
      this.overrideContext(types.b_expr);
      return this.parseObj(false, refDestructuringErrors);
    case types$1._function:
      node = this.startNode();
      this.next();
      return this.parseFunction(node, 0);
    case types$1._class:
      return this.parseClass(this.startNode(), false);
    case types$1._new:
      return this.parseNew();
    case types$1.backQuote:
      return this.parseTemplate();
    case types$1._import:
      if (this.options.ecmaVersion >= 11) {
        return this.parseExprImport(forNew);
      } else {
        return this.unexpected();
      }
    default:
      return this.parseExprAtomDefault();
  }
};
pp$5.parseExprAtomDefault = function() {
  this.unexpected();
};
pp$5.parseExprImport = function(forNew) {
  var node = this.startNode();
  if (this.containsEsc) {
    this.raiseRecoverable(this.start, "Escape sequence in keyword import");
  }
  this.next();
  if (this.type === types$1.parenL && !forNew) {
    return this.parseDynamicImport(node);
  } else if (this.type === types$1.dot) {
    var meta = this.startNodeAt(node.start, node.loc && node.loc.start);
    meta.name = "import";
    node.meta = this.finishNode(meta, "Identifier");
    return this.parseImportMeta(node);
  } else {
    this.unexpected();
  }
};
pp$5.parseDynamicImport = function(node) {
  this.next();
  node.source = this.parseMaybeAssign();
  if (this.options.ecmaVersion >= 16) {
    if (!this.eat(types$1.parenR)) {
      this.expect(types$1.comma);
      if (!this.afterTrailingComma(types$1.parenR)) {
        node.options = this.parseMaybeAssign();
        if (!this.eat(types$1.parenR)) {
          this.expect(types$1.comma);
          if (!this.afterTrailingComma(types$1.parenR)) {
            this.unexpected();
          }
        }
      } else {
        node.options = null;
      }
    } else {
      node.options = null;
    }
  } else {
    if (!this.eat(types$1.parenR)) {
      var errorPos = this.start;
      if (this.eat(types$1.comma) && this.eat(types$1.parenR)) {
        this.raiseRecoverable(errorPos, "Trailing comma is not allowed in import()");
      } else {
        this.unexpected(errorPos);
      }
    }
  }
  return this.finishNode(node, "ImportExpression");
};
pp$5.parseImportMeta = function(node) {
  this.next();
  var containsEsc = this.containsEsc;
  node.property = this.parseIdent(true);
  if (node.property.name !== "meta") {
    this.raiseRecoverable(node.property.start, "The only valid meta property for import is 'import.meta'");
  }
  if (containsEsc) {
    this.raiseRecoverable(node.start, "'import.meta' must not contain escaped characters");
  }
  if (this.options.sourceType !== "module" && !this.options.allowImportExportEverywhere) {
    this.raiseRecoverable(node.start, "Cannot use 'import.meta' outside a module");
  }
  return this.finishNode(node, "MetaProperty");
};
pp$5.parseLiteral = function(value) {
  var node = this.startNode();
  node.value = value;
  node.raw = this.input.slice(this.start, this.end);
  if (node.raw.charCodeAt(node.raw.length - 1) === 110) {
    node.bigint = node.raw.slice(0, -1).replace(/_/g, "");
  }
  this.next();
  return this.finishNode(node, "Literal");
};
pp$5.parseParenExpression = function() {
  this.expect(types$1.parenL);
  var val = this.parseExpression();
  this.expect(types$1.parenR);
  return val;
};
pp$5.shouldParseArrow = function(exprList) {
  return !this.canInsertSemicolon();
};
pp$5.parseParenAndDistinguishExpression = function(canBeArrow, forInit) {
  var startPos = this.start, startLoc = this.startLoc, val, allowTrailingComma = this.options.ecmaVersion >= 8;
  if (this.options.ecmaVersion >= 6) {
    this.next();
    var innerStartPos = this.start, innerStartLoc = this.startLoc;
    var exprList = [], first = true, lastIsComma = false;
    var refDestructuringErrors = new DestructuringErrors(), oldYieldPos = this.yieldPos, oldAwaitPos = this.awaitPos, spreadStart;
    this.yieldPos = 0;
    this.awaitPos = 0;
    while (this.type !== types$1.parenR) {
      first ? first = false : this.expect(types$1.comma);
      if (allowTrailingComma && this.afterTrailingComma(types$1.parenR, true)) {
        lastIsComma = true;
        break;
      } else if (this.type === types$1.ellipsis) {
        spreadStart = this.start;
        exprList.push(this.parseParenItem(this.parseRestBinding()));
        if (this.type === types$1.comma) {
          this.raiseRecoverable(
            this.start,
            "Comma is not permitted after the rest element"
          );
        }
        break;
      } else {
        exprList.push(this.parseMaybeAssign(false, refDestructuringErrors, this.parseParenItem));
      }
    }
    var innerEndPos = this.lastTokEnd, innerEndLoc = this.lastTokEndLoc;
    this.expect(types$1.parenR);
    if (canBeArrow && this.shouldParseArrow(exprList) && this.eat(types$1.arrow)) {
      this.checkPatternErrors(refDestructuringErrors, false);
      this.checkYieldAwaitInDefaultParams();
      this.yieldPos = oldYieldPos;
      this.awaitPos = oldAwaitPos;
      return this.parseParenArrowList(startPos, startLoc, exprList, forInit);
    }
    if (!exprList.length || lastIsComma) {
      this.unexpected(this.lastTokStart);
    }
    if (spreadStart) {
      this.unexpected(spreadStart);
    }
    this.checkExpressionErrors(refDestructuringErrors, true);
    this.yieldPos = oldYieldPos || this.yieldPos;
    this.awaitPos = oldAwaitPos || this.awaitPos;
    if (exprList.length > 1) {
      val = this.startNodeAt(innerStartPos, innerStartLoc);
      val.expressions = exprList;
      this.finishNodeAt(val, "SequenceExpression", innerEndPos, innerEndLoc);
    } else {
      val = exprList[0];
    }
  } else {
    val = this.parseParenExpression();
  }
  if (this.options.preserveParens) {
    var par = this.startNodeAt(startPos, startLoc);
    par.expression = val;
    return this.finishNode(par, "ParenthesizedExpression");
  } else {
    return val;
  }
};
pp$5.parseParenItem = function(item) {
  return item;
};
pp$5.parseParenArrowList = function(startPos, startLoc, exprList, forInit) {
  return this.parseArrowExpression(this.startNodeAt(startPos, startLoc), exprList, false, forInit);
};
var empty = [];
pp$5.parseNew = function() {
  if (this.containsEsc) {
    this.raiseRecoverable(this.start, "Escape sequence in keyword new");
  }
  var node = this.startNode();
  this.next();
  if (this.options.ecmaVersion >= 6 && this.type === types$1.dot) {
    var meta = this.startNodeAt(node.start, node.loc && node.loc.start);
    meta.name = "new";
    node.meta = this.finishNode(meta, "Identifier");
    this.next();
    var containsEsc = this.containsEsc;
    node.property = this.parseIdent(true);
    if (node.property.name !== "target") {
      this.raiseRecoverable(node.property.start, "The only valid meta property for new is 'new.target'");
    }
    if (containsEsc) {
      this.raiseRecoverable(node.start, "'new.target' must not contain escaped characters");
    }
    if (!this.allowNewDotTarget) {
      this.raiseRecoverable(node.start, "'new.target' can only be used in functions and class static block");
    }
    return this.finishNode(node, "MetaProperty");
  }
  var startPos = this.start, startLoc = this.startLoc;
  node.callee = this.parseSubscripts(this.parseExprAtom(null, false, true), startPos, startLoc, true, false);
  if (this.eat(types$1.parenL)) {
    node.arguments = this.parseExprList(types$1.parenR, this.options.ecmaVersion >= 8, false);
  } else {
    node.arguments = empty;
  }
  return this.finishNode(node, "NewExpression");
};
pp$5.parseTemplateElement = function(ref2) {
  var isTagged = ref2.isTagged;
  var elem = this.startNode();
  if (this.type === types$1.invalidTemplate) {
    if (!isTagged) {
      this.raiseRecoverable(this.start, "Bad escape sequence in untagged template literal");
    }
    elem.value = {
      raw: this.value.replace(/\r\n?/g, "\n"),
      cooked: null
    };
  } else {
    elem.value = {
      raw: this.input.slice(this.start, this.end).replace(/\r\n?/g, "\n"),
      cooked: this.value
    };
  }
  this.next();
  elem.tail = this.type === types$1.backQuote;
  return this.finishNode(elem, "TemplateElement");
};
pp$5.parseTemplate = function(ref2) {
  if (ref2 === void 0) ref2 = {};
  var isTagged = ref2.isTagged;
  if (isTagged === void 0) isTagged = false;
  var node = this.startNode();
  this.next();
  node.expressions = [];
  var curElt = this.parseTemplateElement({ isTagged });
  node.quasis = [curElt];
  while (!curElt.tail) {
    if (this.type === types$1.eof) {
      this.raise(this.pos, "Unterminated template literal");
    }
    this.expect(types$1.dollarBraceL);
    node.expressions.push(this.parseExpression());
    this.expect(types$1.braceR);
    node.quasis.push(curElt = this.parseTemplateElement({ isTagged }));
  }
  this.next();
  return this.finishNode(node, "TemplateLiteral");
};
pp$5.isAsyncProp = function(prop) {
  return !prop.computed && prop.key.type === "Identifier" && prop.key.name === "async" && (this.type === types$1.name || this.type === types$1.num || this.type === types$1.string || this.type === types$1.bracketL || this.type.keyword || this.options.ecmaVersion >= 9 && this.type === types$1.star) && !lineBreak.test(this.input.slice(this.lastTokEnd, this.start));
};
pp$5.parseObj = function(isPattern, refDestructuringErrors) {
  var node = this.startNode(), first = true, propHash = {};
  node.properties = [];
  this.next();
  while (!this.eat(types$1.braceR)) {
    if (!first) {
      this.expect(types$1.comma);
      if (this.options.ecmaVersion >= 5 && this.afterTrailingComma(types$1.braceR)) {
        break;
      }
    } else {
      first = false;
    }
    var prop = this.parseProperty(isPattern, refDestructuringErrors);
    if (!isPattern) {
      this.checkPropClash(prop, propHash, refDestructuringErrors);
    }
    node.properties.push(prop);
  }
  return this.finishNode(node, isPattern ? "ObjectPattern" : "ObjectExpression");
};
pp$5.parseProperty = function(isPattern, refDestructuringErrors) {
  var prop = this.startNode(), isGenerator, isAsync, startPos, startLoc;
  if (this.options.ecmaVersion >= 9 && this.eat(types$1.ellipsis)) {
    if (isPattern) {
      prop.argument = this.parseIdent(false);
      if (this.type === types$1.comma) {
        this.raiseRecoverable(this.start, "Comma is not permitted after the rest element");
      }
      return this.finishNode(prop, "RestElement");
    }
    prop.argument = this.parseMaybeAssign(false, refDestructuringErrors);
    if (this.type === types$1.comma && refDestructuringErrors && refDestructuringErrors.trailingComma < 0) {
      refDestructuringErrors.trailingComma = this.start;
    }
    return this.finishNode(prop, "SpreadElement");
  }
  if (this.options.ecmaVersion >= 6) {
    prop.method = false;
    prop.shorthand = false;
    if (isPattern || refDestructuringErrors) {
      startPos = this.start;
      startLoc = this.startLoc;
    }
    if (!isPattern) {
      isGenerator = this.eat(types$1.star);
    }
  }
  var containsEsc = this.containsEsc;
  this.parsePropertyName(prop);
  if (!isPattern && !containsEsc && this.options.ecmaVersion >= 8 && !isGenerator && this.isAsyncProp(prop)) {
    isAsync = true;
    isGenerator = this.options.ecmaVersion >= 9 && this.eat(types$1.star);
    this.parsePropertyName(prop);
  } else {
    isAsync = false;
  }
  this.parsePropertyValue(prop, isPattern, isGenerator, isAsync, startPos, startLoc, refDestructuringErrors, containsEsc);
  return this.finishNode(prop, "Property");
};
pp$5.parseGetterSetter = function(prop) {
  prop.kind = prop.key.name;
  this.parsePropertyName(prop);
  prop.value = this.parseMethod(false);
  var paramCount = prop.kind === "get" ? 0 : 1;
  if (prop.value.params.length !== paramCount) {
    var start = prop.value.start;
    if (prop.kind === "get") {
      this.raiseRecoverable(start, "getter should have no params");
    } else {
      this.raiseRecoverable(start, "setter should have exactly one param");
    }
  } else {
    if (prop.kind === "set" && prop.value.params[0].type === "RestElement") {
      this.raiseRecoverable(prop.value.params[0].start, "Setter cannot use rest params");
    }
  }
};
pp$5.parsePropertyValue = function(prop, isPattern, isGenerator, isAsync, startPos, startLoc, refDestructuringErrors, containsEsc) {
  if ((isGenerator || isAsync) && this.type === types$1.colon) {
    this.unexpected();
  }
  if (this.eat(types$1.colon)) {
    prop.value = isPattern ? this.parseMaybeDefault(this.start, this.startLoc) : this.parseMaybeAssign(false, refDestructuringErrors);
    prop.kind = "init";
  } else if (this.options.ecmaVersion >= 6 && this.type === types$1.parenL) {
    if (isPattern) {
      this.unexpected();
    }
    prop.kind = "init";
    prop.method = true;
    prop.value = this.parseMethod(isGenerator, isAsync);
  } else if (!isPattern && !containsEsc && this.options.ecmaVersion >= 5 && !prop.computed && prop.key.type === "Identifier" && (prop.key.name === "get" || prop.key.name === "set") && (this.type !== types$1.comma && this.type !== types$1.braceR && this.type !== types$1.eq)) {
    if (isGenerator || isAsync) {
      this.unexpected();
    }
    this.parseGetterSetter(prop);
  } else if (this.options.ecmaVersion >= 6 && !prop.computed && prop.key.type === "Identifier") {
    if (isGenerator || isAsync) {
      this.unexpected();
    }
    this.checkUnreserved(prop.key);
    if (prop.key.name === "await" && !this.awaitIdentPos) {
      this.awaitIdentPos = startPos;
    }
    prop.kind = "init";
    if (isPattern) {
      prop.value = this.parseMaybeDefault(startPos, startLoc, this.copyNode(prop.key));
    } else if (this.type === types$1.eq && refDestructuringErrors) {
      if (refDestructuringErrors.shorthandAssign < 0) {
        refDestructuringErrors.shorthandAssign = this.start;
      }
      prop.value = this.parseMaybeDefault(startPos, startLoc, this.copyNode(prop.key));
    } else {
      prop.value = this.copyNode(prop.key);
    }
    prop.shorthand = true;
  } else {
    this.unexpected();
  }
};
pp$5.parsePropertyName = function(prop) {
  if (this.options.ecmaVersion >= 6) {
    if (this.eat(types$1.bracketL)) {
      prop.computed = true;
      prop.key = this.parseMaybeAssign();
      this.expect(types$1.bracketR);
      return prop.key;
    } else {
      prop.computed = false;
    }
  }
  return prop.key = this.type === types$1.num || this.type === types$1.string ? this.parseExprAtom() : this.parseIdent(this.options.allowReserved !== "never");
};
pp$5.initFunction = function(node) {
  node.id = null;
  if (this.options.ecmaVersion >= 6) {
    node.generator = node.expression = false;
  }
  if (this.options.ecmaVersion >= 8) {
    node.async = false;
  }
};
pp$5.parseMethod = function(isGenerator, isAsync, allowDirectSuper) {
  var node = this.startNode(), oldYieldPos = this.yieldPos, oldAwaitPos = this.awaitPos, oldAwaitIdentPos = this.awaitIdentPos;
  this.initFunction(node);
  if (this.options.ecmaVersion >= 6) {
    node.generator = isGenerator;
  }
  if (this.options.ecmaVersion >= 8) {
    node.async = !!isAsync;
  }
  this.yieldPos = 0;
  this.awaitPos = 0;
  this.awaitIdentPos = 0;
  this.enterScope(functionFlags(isAsync, node.generator) | SCOPE_SUPER | (allowDirectSuper ? SCOPE_DIRECT_SUPER : 0));
  this.expect(types$1.parenL);
  node.params = this.parseBindingList(types$1.parenR, false, this.options.ecmaVersion >= 8);
  this.checkYieldAwaitInDefaultParams();
  this.parseFunctionBody(node, false, true, false);
  this.yieldPos = oldYieldPos;
  this.awaitPos = oldAwaitPos;
  this.awaitIdentPos = oldAwaitIdentPos;
  return this.finishNode(node, "FunctionExpression");
};
pp$5.parseArrowExpression = function(node, params, isAsync, forInit) {
  var oldYieldPos = this.yieldPos, oldAwaitPos = this.awaitPos, oldAwaitIdentPos = this.awaitIdentPos;
  this.enterScope(functionFlags(isAsync, false) | SCOPE_ARROW);
  this.initFunction(node);
  if (this.options.ecmaVersion >= 8) {
    node.async = !!isAsync;
  }
  this.yieldPos = 0;
  this.awaitPos = 0;
  this.awaitIdentPos = 0;
  node.params = this.toAssignableList(params, true);
  this.parseFunctionBody(node, true, false, forInit);
  this.yieldPos = oldYieldPos;
  this.awaitPos = oldAwaitPos;
  this.awaitIdentPos = oldAwaitIdentPos;
  return this.finishNode(node, "ArrowFunctionExpression");
};
pp$5.parseFunctionBody = function(node, isArrowFunction, isMethod, forInit) {
  var isExpression = isArrowFunction && this.type !== types$1.braceL;
  var oldStrict = this.strict, useStrict = false;
  if (isExpression) {
    node.body = this.parseMaybeAssign(forInit);
    node.expression = true;
    this.checkParams(node, false);
  } else {
    var nonSimple = this.options.ecmaVersion >= 7 && !this.isSimpleParamList(node.params);
    if (!oldStrict || nonSimple) {
      useStrict = this.strictDirective(this.end);
      if (useStrict && nonSimple) {
        this.raiseRecoverable(node.start, "Illegal 'use strict' directive in function with non-simple parameter list");
      }
    }
    var oldLabels = this.labels;
    this.labels = [];
    if (useStrict) {
      this.strict = true;
    }
    this.checkParams(node, !oldStrict && !useStrict && !isArrowFunction && !isMethod && this.isSimpleParamList(node.params));
    if (this.strict && node.id) {
      this.checkLValSimple(node.id, BIND_OUTSIDE);
    }
    node.body = this.parseBlock(false, void 0, useStrict && !oldStrict);
    node.expression = false;
    this.adaptDirectivePrologue(node.body.body);
    this.labels = oldLabels;
  }
  this.exitScope();
};
pp$5.isSimpleParamList = function(params) {
  for (var i = 0, list = params; i < list.length; i += 1) {
    var param = list[i];
    if (param.type !== "Identifier") {
      return false;
    }
  }
  return true;
};
pp$5.checkParams = function(node, allowDuplicates) {
  var nameHash = /* @__PURE__ */ Object.create(null);
  for (var i = 0, list = node.params; i < list.length; i += 1) {
    var param = list[i];
    this.checkLValInnerPattern(param, BIND_VAR, allowDuplicates ? null : nameHash);
  }
};
pp$5.parseExprList = function(close, allowTrailingComma, allowEmpty, refDestructuringErrors) {
  var elts = [], first = true;
  while (!this.eat(close)) {
    if (!first) {
      this.expect(types$1.comma);
      if (allowTrailingComma && this.afterTrailingComma(close)) {
        break;
      }
    } else {
      first = false;
    }
    var elt = void 0;
    if (allowEmpty && this.type === types$1.comma) {
      elt = null;
    } else if (this.type === types$1.ellipsis) {
      elt = this.parseSpread(refDestructuringErrors);
      if (refDestructuringErrors && this.type === types$1.comma && refDestructuringErrors.trailingComma < 0) {
        refDestructuringErrors.trailingComma = this.start;
      }
    } else {
      elt = this.parseMaybeAssign(false, refDestructuringErrors);
    }
    elts.push(elt);
  }
  return elts;
};
pp$5.checkUnreserved = function(ref2) {
  var start = ref2.start;
  var end = ref2.end;
  var name = ref2.name;
  if (this.inGenerator && name === "yield") {
    this.raiseRecoverable(start, "Cannot use 'yield' as identifier inside a generator");
  }
  if (this.inAsync && name === "await") {
    this.raiseRecoverable(start, "Cannot use 'await' as identifier inside an async function");
  }
  if (this.currentThisScope().inClassFieldInit && name === "arguments") {
    this.raiseRecoverable(start, "Cannot use 'arguments' in class field initializer");
  }
  if (this.inClassStaticBlock && (name === "arguments" || name === "await")) {
    this.raise(start, "Cannot use " + name + " in class static initialization block");
  }
  if (this.keywords.test(name)) {
    this.raise(start, "Unexpected keyword '" + name + "'");
  }
  if (this.options.ecmaVersion < 6 && this.input.slice(start, end).indexOf("\\") !== -1) {
    return;
  }
  var re = this.strict ? this.reservedWordsStrict : this.reservedWords;
  if (re.test(name)) {
    if (!this.inAsync && name === "await") {
      this.raiseRecoverable(start, "Cannot use keyword 'await' outside an async function");
    }
    this.raiseRecoverable(start, "The keyword '" + name + "' is reserved");
  }
};
pp$5.parseIdent = function(liberal) {
  var node = this.parseIdentNode();
  this.next(!!liberal);
  this.finishNode(node, "Identifier");
  if (!liberal) {
    this.checkUnreserved(node);
    if (node.name === "await" && !this.awaitIdentPos) {
      this.awaitIdentPos = node.start;
    }
  }
  return node;
};
pp$5.parseIdentNode = function() {
  var node = this.startNode();
  if (this.type === types$1.name) {
    node.name = this.value;
  } else if (this.type.keyword) {
    node.name = this.type.keyword;
    if ((node.name === "class" || node.name === "function") && (this.lastTokEnd !== this.lastTokStart + 1 || this.input.charCodeAt(this.lastTokStart) !== 46)) {
      this.context.pop();
    }
    this.type = types$1.name;
  } else {
    this.unexpected();
  }
  return node;
};
pp$5.parsePrivateIdent = function() {
  var node = this.startNode();
  if (this.type === types$1.privateId) {
    node.name = this.value;
  } else {
    this.unexpected();
  }
  this.next();
  this.finishNode(node, "PrivateIdentifier");
  if (this.options.checkPrivateFields) {
    if (this.privateNameStack.length === 0) {
      this.raise(node.start, "Private field '#" + node.name + "' must be declared in an enclosing class");
    } else {
      this.privateNameStack[this.privateNameStack.length - 1].used.push(node);
    }
  }
  return node;
};
pp$5.parseYield = function(forInit) {
  if (!this.yieldPos) {
    this.yieldPos = this.start;
  }
  var node = this.startNode();
  this.next();
  if (this.type === types$1.semi || this.canInsertSemicolon() || this.type !== types$1.star && !this.type.startsExpr) {
    node.delegate = false;
    node.argument = null;
  } else {
    node.delegate = this.eat(types$1.star);
    node.argument = this.parseMaybeAssign(forInit);
  }
  return this.finishNode(node, "YieldExpression");
};
pp$5.parseAwait = function(forInit) {
  if (!this.awaitPos) {
    this.awaitPos = this.start;
  }
  var node = this.startNode();
  this.next();
  node.argument = this.parseMaybeUnary(null, true, false, forInit);
  return this.finishNode(node, "AwaitExpression");
};
var pp$4 = Parser.prototype;
pp$4.raise = function(pos, message) {
  var loc = getLineInfo(this.input, pos);
  message += " (" + loc.line + ":" + loc.column + ")";
  var err = new SyntaxError(message);
  err.pos = pos;
  err.loc = loc;
  err.raisedAt = this.pos;
  throw err;
};
pp$4.raiseRecoverable = pp$4.raise;
pp$4.curPosition = function() {
  if (this.options.locations) {
    return new Position(this.curLine, this.pos - this.lineStart);
  }
};
var pp$3 = Parser.prototype;
var Scope = function Scope2(flags) {
  this.flags = flags;
  this.var = [];
  this.lexical = [];
  this.functions = [];
  this.inClassFieldInit = false;
};
pp$3.enterScope = function(flags) {
  this.scopeStack.push(new Scope(flags));
};
pp$3.exitScope = function() {
  this.scopeStack.pop();
};
pp$3.treatFunctionsAsVarInScope = function(scope) {
  return scope.flags & SCOPE_FUNCTION || !this.inModule && scope.flags & SCOPE_TOP;
};
pp$3.declareName = function(name, bindingType, pos) {
  var redeclared = false;
  if (bindingType === BIND_LEXICAL) {
    var scope = this.currentScope();
    redeclared = scope.lexical.indexOf(name) > -1 || scope.functions.indexOf(name) > -1 || scope.var.indexOf(name) > -1;
    scope.lexical.push(name);
    if (this.inModule && scope.flags & SCOPE_TOP) {
      delete this.undefinedExports[name];
    }
  } else if (bindingType === BIND_SIMPLE_CATCH) {
    var scope$1 = this.currentScope();
    scope$1.lexical.push(name);
  } else if (bindingType === BIND_FUNCTION) {
    var scope$2 = this.currentScope();
    if (this.treatFunctionsAsVar) {
      redeclared = scope$2.lexical.indexOf(name) > -1;
    } else {
      redeclared = scope$2.lexical.indexOf(name) > -1 || scope$2.var.indexOf(name) > -1;
    }
    scope$2.functions.push(name);
  } else {
    for (var i = this.scopeStack.length - 1; i >= 0; --i) {
      var scope$3 = this.scopeStack[i];
      if (scope$3.lexical.indexOf(name) > -1 && !(scope$3.flags & SCOPE_SIMPLE_CATCH && scope$3.lexical[0] === name) || !this.treatFunctionsAsVarInScope(scope$3) && scope$3.functions.indexOf(name) > -1) {
        redeclared = true;
        break;
      }
      scope$3.var.push(name);
      if (this.inModule && scope$3.flags & SCOPE_TOP) {
        delete this.undefinedExports[name];
      }
      if (scope$3.flags & SCOPE_VAR) {
        break;
      }
    }
  }
  if (redeclared) {
    this.raiseRecoverable(pos, "Identifier '" + name + "' has already been declared");
  }
};
pp$3.checkLocalExport = function(id) {
  if (this.scopeStack[0].lexical.indexOf(id.name) === -1 && this.scopeStack[0].var.indexOf(id.name) === -1) {
    this.undefinedExports[id.name] = id;
  }
};
pp$3.currentScope = function() {
  return this.scopeStack[this.scopeStack.length - 1];
};
pp$3.currentVarScope = function() {
  for (var i = this.scopeStack.length - 1; ; i--) {
    var scope = this.scopeStack[i];
    if (scope.flags & SCOPE_VAR) {
      return scope;
    }
  }
};
pp$3.currentThisScope = function() {
  for (var i = this.scopeStack.length - 1; ; i--) {
    var scope = this.scopeStack[i];
    if (scope.flags & SCOPE_VAR && !(scope.flags & SCOPE_ARROW)) {
      return scope;
    }
  }
};
var Node = function Node2(parser, pos, loc) {
  this.type = "";
  this.start = pos;
  this.end = 0;
  if (parser.options.locations) {
    this.loc = new SourceLocation(parser, loc);
  }
  if (parser.options.directSourceFile) {
    this.sourceFile = parser.options.directSourceFile;
  }
  if (parser.options.ranges) {
    this.range = [pos, 0];
  }
};
var pp$2 = Parser.prototype;
pp$2.startNode = function() {
  return new Node(this, this.start, this.startLoc);
};
pp$2.startNodeAt = function(pos, loc) {
  return new Node(this, pos, loc);
};
function finishNodeAt(node, type, pos, loc) {
  node.type = type;
  node.end = pos;
  if (this.options.locations) {
    node.loc.end = loc;
  }
  if (this.options.ranges) {
    node.range[1] = pos;
  }
  return node;
}
pp$2.finishNode = function(node, type) {
  return finishNodeAt.call(this, node, type, this.lastTokEnd, this.lastTokEndLoc);
};
pp$2.finishNodeAt = function(node, type, pos, loc) {
  return finishNodeAt.call(this, node, type, pos, loc);
};
pp$2.copyNode = function(node) {
  var newNode = new Node(this, node.start, this.startLoc);
  for (var prop in node) {
    newNode[prop] = node[prop];
  }
  return newNode;
};
var scriptValuesAddedInUnicode = "Gara Garay Gukh Gurung_Khema Hrkt Katakana_Or_Hiragana Kawi Kirat_Rai Krai Nag_Mundari Nagm Ol_Onal Onao Sunu Sunuwar Todhri Todr Tulu_Tigalari Tutg Unknown Zzzz";
var ecma9BinaryProperties = "ASCII ASCII_Hex_Digit AHex Alphabetic Alpha Any Assigned Bidi_Control Bidi_C Bidi_Mirrored Bidi_M Case_Ignorable CI Cased Changes_When_Casefolded CWCF Changes_When_Casemapped CWCM Changes_When_Lowercased CWL Changes_When_NFKC_Casefolded CWKCF Changes_When_Titlecased CWT Changes_When_Uppercased CWU Dash Default_Ignorable_Code_Point DI Deprecated Dep Diacritic Dia Emoji Emoji_Component Emoji_Modifier Emoji_Modifier_Base Emoji_Presentation Extender Ext Grapheme_Base Gr_Base Grapheme_Extend Gr_Ext Hex_Digit Hex IDS_Binary_Operator IDSB IDS_Trinary_Operator IDST ID_Continue IDC ID_Start IDS Ideographic Ideo Join_Control Join_C Logical_Order_Exception LOE Lowercase Lower Math Noncharacter_Code_Point NChar Pattern_Syntax Pat_Syn Pattern_White_Space Pat_WS Quotation_Mark QMark Radical Regional_Indicator RI Sentence_Terminal STerm Soft_Dotted SD Terminal_Punctuation Term Unified_Ideograph UIdeo Uppercase Upper Variation_Selector VS White_Space space XID_Continue XIDC XID_Start XIDS";
var ecma10BinaryProperties = ecma9BinaryProperties + " Extended_Pictographic";
var ecma11BinaryProperties = ecma10BinaryProperties;
var ecma12BinaryProperties = ecma11BinaryProperties + " EBase EComp EMod EPres ExtPict";
var ecma13BinaryProperties = ecma12BinaryProperties;
var ecma14BinaryProperties = ecma13BinaryProperties;
var unicodeBinaryProperties = {
  9: ecma9BinaryProperties,
  10: ecma10BinaryProperties,
  11: ecma11BinaryProperties,
  12: ecma12BinaryProperties,
  13: ecma13BinaryProperties,
  14: ecma14BinaryProperties
};
var ecma14BinaryPropertiesOfStrings = "Basic_Emoji Emoji_Keycap_Sequence RGI_Emoji_Modifier_Sequence RGI_Emoji_Flag_Sequence RGI_Emoji_Tag_Sequence RGI_Emoji_ZWJ_Sequence RGI_Emoji";
var unicodeBinaryPropertiesOfStrings = {
  9: "",
  10: "",
  11: "",
  12: "",
  13: "",
  14: ecma14BinaryPropertiesOfStrings
};
var unicodeGeneralCategoryValues = "Cased_Letter LC Close_Punctuation Pe Connector_Punctuation Pc Control Cc cntrl Currency_Symbol Sc Dash_Punctuation Pd Decimal_Number Nd digit Enclosing_Mark Me Final_Punctuation Pf Format Cf Initial_Punctuation Pi Letter L Letter_Number Nl Line_Separator Zl Lowercase_Letter Ll Mark M Combining_Mark Math_Symbol Sm Modifier_Letter Lm Modifier_Symbol Sk Nonspacing_Mark Mn Number N Open_Punctuation Ps Other C Other_Letter Lo Other_Number No Other_Punctuation Po Other_Symbol So Paragraph_Separator Zp Private_Use Co Punctuation P punct Separator Z Space_Separator Zs Spacing_Mark Mc Surrogate Cs Symbol S Titlecase_Letter Lt Unassigned Cn Uppercase_Letter Lu";
var ecma9ScriptValues = "Adlam Adlm Ahom Anatolian_Hieroglyphs Hluw Arabic Arab Armenian Armn Avestan Avst Balinese Bali Bamum Bamu Bassa_Vah Bass Batak Batk Bengali Beng Bhaiksuki Bhks Bopomofo Bopo Brahmi Brah Braille Brai Buginese Bugi Buhid Buhd Canadian_Aboriginal Cans Carian Cari Caucasian_Albanian Aghb Chakma Cakm Cham Cham Cherokee Cher Common Zyyy Coptic Copt Qaac Cuneiform Xsux Cypriot Cprt Cyrillic Cyrl Deseret Dsrt Devanagari Deva Duployan Dupl Egyptian_Hieroglyphs Egyp Elbasan Elba Ethiopic Ethi Georgian Geor Glagolitic Glag Gothic Goth Grantha Gran Greek Grek Gujarati Gujr Gurmukhi Guru Han Hani Hangul Hang Hanunoo Hano Hatran Hatr Hebrew Hebr Hiragana Hira Imperial_Aramaic Armi Inherited Zinh Qaai Inscriptional_Pahlavi Phli Inscriptional_Parthian Prti Javanese Java Kaithi Kthi Kannada Knda Katakana Kana Kayah_Li Kali Kharoshthi Khar Khmer Khmr Khojki Khoj Khudawadi Sind Lao Laoo Latin Latn Lepcha Lepc Limbu Limb Linear_A Lina Linear_B Linb Lisu Lisu Lycian Lyci Lydian Lydi Mahajani Mahj Malayalam Mlym Mandaic Mand Manichaean Mani Marchen Marc Masaram_Gondi Gonm Meetei_Mayek Mtei Mende_Kikakui Mend Meroitic_Cursive Merc Meroitic_Hieroglyphs Mero Miao Plrd Modi Mongolian Mong Mro Mroo Multani Mult Myanmar Mymr Nabataean Nbat New_Tai_Lue Talu Newa Newa Nko Nkoo Nushu Nshu Ogham Ogam Ol_Chiki Olck Old_Hungarian Hung Old_Italic Ital Old_North_Arabian Narb Old_Permic Perm Old_Persian Xpeo Old_South_Arabian Sarb Old_Turkic Orkh Oriya Orya Osage Osge Osmanya Osma Pahawh_Hmong Hmng Palmyrene Palm Pau_Cin_Hau Pauc Phags_Pa Phag Phoenician Phnx Psalter_Pahlavi Phlp Rejang Rjng Runic Runr Samaritan Samr Saurashtra Saur Sharada Shrd Shavian Shaw Siddham Sidd SignWriting Sgnw Sinhala Sinh Sora_Sompeng Sora Soyombo Soyo Sundanese Sund Syloti_Nagri Sylo Syriac Syrc Tagalog Tglg Tagbanwa Tagb Tai_Le Tale Tai_Tham Lana Tai_Viet Tavt Takri Takr Tamil Taml Tangut Tang Telugu Telu Thaana Thaa Thai Thai Tibetan Tibt Tifinagh Tfng Tirhuta Tirh Ugaritic Ugar Vai Vaii Warang_Citi Wara Yi Yiii Zanabazar_Square Zanb";
var ecma10ScriptValues = ecma9ScriptValues + " Dogra Dogr Gunjala_Gondi Gong Hanifi_Rohingya Rohg Makasar Maka Medefaidrin Medf Old_Sogdian Sogo Sogdian Sogd";
var ecma11ScriptValues = ecma10ScriptValues + " Elymaic Elym Nandinagari Nand Nyiakeng_Puachue_Hmong Hmnp Wancho Wcho";
var ecma12ScriptValues = ecma11ScriptValues + " Chorasmian Chrs Diak Dives_Akuru Khitan_Small_Script Kits Yezi Yezidi";
var ecma13ScriptValues = ecma12ScriptValues + " Cypro_Minoan Cpmn Old_Uyghur Ougr Tangsa Tnsa Toto Vithkuqi Vith";
var ecma14ScriptValues = ecma13ScriptValues + " " + scriptValuesAddedInUnicode;
var unicodeScriptValues = {
  9: ecma9ScriptValues,
  10: ecma10ScriptValues,
  11: ecma11ScriptValues,
  12: ecma12ScriptValues,
  13: ecma13ScriptValues,
  14: ecma14ScriptValues
};
var data = {};
function buildUnicodeData(ecmaVersion) {
  var d = data[ecmaVersion] = {
    binary: wordsRegexp(unicodeBinaryProperties[ecmaVersion] + " " + unicodeGeneralCategoryValues),
    binaryOfStrings: wordsRegexp(unicodeBinaryPropertiesOfStrings[ecmaVersion]),
    nonBinary: {
      General_Category: wordsRegexp(unicodeGeneralCategoryValues),
      Script: wordsRegexp(unicodeScriptValues[ecmaVersion])
    }
  };
  d.nonBinary.Script_Extensions = d.nonBinary.Script;
  d.nonBinary.gc = d.nonBinary.General_Category;
  d.nonBinary.sc = d.nonBinary.Script;
  d.nonBinary.scx = d.nonBinary.Script_Extensions;
}
for (i = 0, list = [9, 10, 11, 12, 13, 14]; i < list.length; i += 1) {
  ecmaVersion = list[i];
  buildUnicodeData(ecmaVersion);
}
var ecmaVersion;
var i;
var list;
var pp$1 = Parser.prototype;
var BranchID = function BranchID2(parent, base) {
  this.parent = parent;
  this.base = base || this;
};
BranchID.prototype.separatedFrom = function separatedFrom(alt) {
  for (var self2 = this; self2; self2 = self2.parent) {
    for (var other = alt; other; other = other.parent) {
      if (self2.base === other.base && self2 !== other) {
        return true;
      }
    }
  }
  return false;
};
BranchID.prototype.sibling = function sibling() {
  return new BranchID(this.parent, this.base);
};
var RegExpValidationState = function RegExpValidationState2(parser) {
  this.parser = parser;
  this.validFlags = "gim" + (parser.options.ecmaVersion >= 6 ? "uy" : "") + (parser.options.ecmaVersion >= 9 ? "s" : "") + (parser.options.ecmaVersion >= 13 ? "d" : "") + (parser.options.ecmaVersion >= 15 ? "v" : "");
  this.unicodeProperties = data[parser.options.ecmaVersion >= 14 ? 14 : parser.options.ecmaVersion];
  this.source = "";
  this.flags = "";
  this.start = 0;
  this.switchU = false;
  this.switchV = false;
  this.switchN = false;
  this.pos = 0;
  this.lastIntValue = 0;
  this.lastStringValue = "";
  this.lastAssertionIsQuantifiable = false;
  this.numCapturingParens = 0;
  this.maxBackReference = 0;
  this.groupNames = /* @__PURE__ */ Object.create(null);
  this.backReferenceNames = [];
  this.branchID = null;
};
RegExpValidationState.prototype.reset = function reset(start, pattern, flags) {
  var unicodeSets = flags.indexOf("v") !== -1;
  var unicode = flags.indexOf("u") !== -1;
  this.start = start | 0;
  this.source = pattern + "";
  this.flags = flags;
  if (unicodeSets && this.parser.options.ecmaVersion >= 15) {
    this.switchU = true;
    this.switchV = true;
    this.switchN = true;
  } else {
    this.switchU = unicode && this.parser.options.ecmaVersion >= 6;
    this.switchV = false;
    this.switchN = unicode && this.parser.options.ecmaVersion >= 9;
  }
};
RegExpValidationState.prototype.raise = function raise(message) {
  this.parser.raiseRecoverable(this.start, "Invalid regular expression: /" + this.source + "/: " + message);
};
RegExpValidationState.prototype.at = function at(i, forceU) {
  if (forceU === void 0) forceU = false;
  var s = this.source;
  var l = s.length;
  if (i >= l) {
    return -1;
  }
  var c = s.charCodeAt(i);
  if (!(forceU || this.switchU) || c <= 55295 || c >= 57344 || i + 1 >= l) {
    return c;
  }
  var next = s.charCodeAt(i + 1);
  return next >= 56320 && next <= 57343 ? (c << 10) + next - 56613888 : c;
};
RegExpValidationState.prototype.nextIndex = function nextIndex(i, forceU) {
  if (forceU === void 0) forceU = false;
  var s = this.source;
  var l = s.length;
  if (i >= l) {
    return l;
  }
  var c = s.charCodeAt(i), next;
  if (!(forceU || this.switchU) || c <= 55295 || c >= 57344 || i + 1 >= l || (next = s.charCodeAt(i + 1)) < 56320 || next > 57343) {
    return i + 1;
  }
  return i + 2;
};
RegExpValidationState.prototype.current = function current(forceU) {
  if (forceU === void 0) forceU = false;
  return this.at(this.pos, forceU);
};
RegExpValidationState.prototype.lookahead = function lookahead(forceU) {
  if (forceU === void 0) forceU = false;
  return this.at(this.nextIndex(this.pos, forceU), forceU);
};
RegExpValidationState.prototype.advance = function advance(forceU) {
  if (forceU === void 0) forceU = false;
  this.pos = this.nextIndex(this.pos, forceU);
};
RegExpValidationState.prototype.eat = function eat(ch, forceU) {
  if (forceU === void 0) forceU = false;
  if (this.current(forceU) === ch) {
    this.advance(forceU);
    return true;
  }
  return false;
};
RegExpValidationState.prototype.eatChars = function eatChars(chs, forceU) {
  if (forceU === void 0) forceU = false;
  var pos = this.pos;
  for (var i = 0, list = chs; i < list.length; i += 1) {
    var ch = list[i];
    var current2 = this.at(pos, forceU);
    if (current2 === -1 || current2 !== ch) {
      return false;
    }
    pos = this.nextIndex(pos, forceU);
  }
  this.pos = pos;
  return true;
};
pp$1.validateRegExpFlags = function(state) {
  var validFlags = state.validFlags;
  var flags = state.flags;
  var u = false;
  var v = false;
  for (var i = 0; i < flags.length; i++) {
    var flag = flags.charAt(i);
    if (validFlags.indexOf(flag) === -1) {
      this.raise(state.start, "Invalid regular expression flag");
    }
    if (flags.indexOf(flag, i + 1) > -1) {
      this.raise(state.start, "Duplicate regular expression flag");
    }
    if (flag === "u") {
      u = true;
    }
    if (flag === "v") {
      v = true;
    }
  }
  if (this.options.ecmaVersion >= 15 && u && v) {
    this.raise(state.start, "Invalid regular expression flag");
  }
};
function hasProp(obj) {
  for (var _ in obj) {
    return true;
  }
  return false;
}
pp$1.validateRegExpPattern = function(state) {
  this.regexp_pattern(state);
  if (!state.switchN && this.options.ecmaVersion >= 9 && hasProp(state.groupNames)) {
    state.switchN = true;
    this.regexp_pattern(state);
  }
};
pp$1.regexp_pattern = function(state) {
  state.pos = 0;
  state.lastIntValue = 0;
  state.lastStringValue = "";
  state.lastAssertionIsQuantifiable = false;
  state.numCapturingParens = 0;
  state.maxBackReference = 0;
  state.groupNames = /* @__PURE__ */ Object.create(null);
  state.backReferenceNames.length = 0;
  state.branchID = null;
  this.regexp_disjunction(state);
  if (state.pos !== state.source.length) {
    if (state.eat(
      41
      /* ) */
    )) {
      state.raise("Unmatched ')'");
    }
    if (state.eat(
      93
      /* ] */
    ) || state.eat(
      125
      /* } */
    )) {
      state.raise("Lone quantifier brackets");
    }
  }
  if (state.maxBackReference > state.numCapturingParens) {
    state.raise("Invalid escape");
  }
  for (var i = 0, list = state.backReferenceNames; i < list.length; i += 1) {
    var name = list[i];
    if (!state.groupNames[name]) {
      state.raise("Invalid named capture referenced");
    }
  }
};
pp$1.regexp_disjunction = function(state) {
  var trackDisjunction = this.options.ecmaVersion >= 16;
  if (trackDisjunction) {
    state.branchID = new BranchID(state.branchID, null);
  }
  this.regexp_alternative(state);
  while (state.eat(
    124
    /* | */
  )) {
    if (trackDisjunction) {
      state.branchID = state.branchID.sibling();
    }
    this.regexp_alternative(state);
  }
  if (trackDisjunction) {
    state.branchID = state.branchID.parent;
  }
  if (this.regexp_eatQuantifier(state, true)) {
    state.raise("Nothing to repeat");
  }
  if (state.eat(
    123
    /* { */
  )) {
    state.raise("Lone quantifier brackets");
  }
};
pp$1.regexp_alternative = function(state) {
  while (state.pos < state.source.length && this.regexp_eatTerm(state)) {
  }
};
pp$1.regexp_eatTerm = function(state) {
  if (this.regexp_eatAssertion(state)) {
    if (state.lastAssertionIsQuantifiable && this.regexp_eatQuantifier(state)) {
      if (state.switchU) {
        state.raise("Invalid quantifier");
      }
    }
    return true;
  }
  if (state.switchU ? this.regexp_eatAtom(state) : this.regexp_eatExtendedAtom(state)) {
    this.regexp_eatQuantifier(state);
    return true;
  }
  return false;
};
pp$1.regexp_eatAssertion = function(state) {
  var start = state.pos;
  state.lastAssertionIsQuantifiable = false;
  if (state.eat(
    94
    /* ^ */
  ) || state.eat(
    36
    /* $ */
  )) {
    return true;
  }
  if (state.eat(
    92
    /* \ */
  )) {
    if (state.eat(
      66
      /* B */
    ) || state.eat(
      98
      /* b */
    )) {
      return true;
    }
    state.pos = start;
  }
  if (state.eat(
    40
    /* ( */
  ) && state.eat(
    63
    /* ? */
  )) {
    var lookbehind = false;
    if (this.options.ecmaVersion >= 9) {
      lookbehind = state.eat(
        60
        /* < */
      );
    }
    if (state.eat(
      61
      /* = */
    ) || state.eat(
      33
      /* ! */
    )) {
      this.regexp_disjunction(state);
      if (!state.eat(
        41
        /* ) */
      )) {
        state.raise("Unterminated group");
      }
      state.lastAssertionIsQuantifiable = !lookbehind;
      return true;
    }
  }
  state.pos = start;
  return false;
};
pp$1.regexp_eatQuantifier = function(state, noError) {
  if (noError === void 0) noError = false;
  if (this.regexp_eatQuantifierPrefix(state, noError)) {
    state.eat(
      63
      /* ? */
    );
    return true;
  }
  return false;
};
pp$1.regexp_eatQuantifierPrefix = function(state, noError) {
  return state.eat(
    42
    /* * */
  ) || state.eat(
    43
    /* + */
  ) || state.eat(
    63
    /* ? */
  ) || this.regexp_eatBracedQuantifier(state, noError);
};
pp$1.regexp_eatBracedQuantifier = function(state, noError) {
  var start = state.pos;
  if (state.eat(
    123
    /* { */
  )) {
    var min = 0, max = -1;
    if (this.regexp_eatDecimalDigits(state)) {
      min = state.lastIntValue;
      if (state.eat(
        44
        /* , */
      ) && this.regexp_eatDecimalDigits(state)) {
        max = state.lastIntValue;
      }
      if (state.eat(
        125
        /* } */
      )) {
        if (max !== -1 && max < min && !noError) {
          state.raise("numbers out of order in {} quantifier");
        }
        return true;
      }
    }
    if (state.switchU && !noError) {
      state.raise("Incomplete quantifier");
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatAtom = function(state) {
  return this.regexp_eatPatternCharacters(state) || state.eat(
    46
    /* . */
  ) || this.regexp_eatReverseSolidusAtomEscape(state) || this.regexp_eatCharacterClass(state) || this.regexp_eatUncapturingGroup(state) || this.regexp_eatCapturingGroup(state);
};
pp$1.regexp_eatReverseSolidusAtomEscape = function(state) {
  var start = state.pos;
  if (state.eat(
    92
    /* \ */
  )) {
    if (this.regexp_eatAtomEscape(state)) {
      return true;
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatUncapturingGroup = function(state) {
  var start = state.pos;
  if (state.eat(
    40
    /* ( */
  )) {
    if (state.eat(
      63
      /* ? */
    )) {
      if (this.options.ecmaVersion >= 16) {
        var addModifiers = this.regexp_eatModifiers(state);
        var hasHyphen = state.eat(
          45
          /* - */
        );
        if (addModifiers || hasHyphen) {
          for (var i = 0; i < addModifiers.length; i++) {
            var modifier = addModifiers.charAt(i);
            if (addModifiers.indexOf(modifier, i + 1) > -1) {
              state.raise("Duplicate regular expression modifiers");
            }
          }
          if (hasHyphen) {
            var removeModifiers = this.regexp_eatModifiers(state);
            if (!addModifiers && !removeModifiers && state.current() === 58) {
              state.raise("Invalid regular expression modifiers");
            }
            for (var i$1 = 0; i$1 < removeModifiers.length; i$1++) {
              var modifier$1 = removeModifiers.charAt(i$1);
              if (removeModifiers.indexOf(modifier$1, i$1 + 1) > -1 || addModifiers.indexOf(modifier$1) > -1) {
                state.raise("Duplicate regular expression modifiers");
              }
            }
          }
        }
      }
      if (state.eat(
        58
        /* : */
      )) {
        this.regexp_disjunction(state);
        if (state.eat(
          41
          /* ) */
        )) {
          return true;
        }
        state.raise("Unterminated group");
      }
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatCapturingGroup = function(state) {
  if (state.eat(
    40
    /* ( */
  )) {
    if (this.options.ecmaVersion >= 9) {
      this.regexp_groupSpecifier(state);
    } else if (state.current() === 63) {
      state.raise("Invalid group");
    }
    this.regexp_disjunction(state);
    if (state.eat(
      41
      /* ) */
    )) {
      state.numCapturingParens += 1;
      return true;
    }
    state.raise("Unterminated group");
  }
  return false;
};
pp$1.regexp_eatModifiers = function(state) {
  var modifiers = "";
  var ch = 0;
  while ((ch = state.current()) !== -1 && isRegularExpressionModifier(ch)) {
    modifiers += codePointToString(ch);
    state.advance();
  }
  return modifiers;
};
function isRegularExpressionModifier(ch) {
  return ch === 105 || ch === 109 || ch === 115;
}
pp$1.regexp_eatExtendedAtom = function(state) {
  return state.eat(
    46
    /* . */
  ) || this.regexp_eatReverseSolidusAtomEscape(state) || this.regexp_eatCharacterClass(state) || this.regexp_eatUncapturingGroup(state) || this.regexp_eatCapturingGroup(state) || this.regexp_eatInvalidBracedQuantifier(state) || this.regexp_eatExtendedPatternCharacter(state);
};
pp$1.regexp_eatInvalidBracedQuantifier = function(state) {
  if (this.regexp_eatBracedQuantifier(state, true)) {
    state.raise("Nothing to repeat");
  }
  return false;
};
pp$1.regexp_eatSyntaxCharacter = function(state) {
  var ch = state.current();
  if (isSyntaxCharacter(ch)) {
    state.lastIntValue = ch;
    state.advance();
    return true;
  }
  return false;
};
function isSyntaxCharacter(ch) {
  return ch === 36 || ch >= 40 && ch <= 43 || ch === 46 || ch === 63 || ch >= 91 && ch <= 94 || ch >= 123 && ch <= 125;
}
pp$1.regexp_eatPatternCharacters = function(state) {
  var start = state.pos;
  var ch = 0;
  while ((ch = state.current()) !== -1 && !isSyntaxCharacter(ch)) {
    state.advance();
  }
  return state.pos !== start;
};
pp$1.regexp_eatExtendedPatternCharacter = function(state) {
  var ch = state.current();
  if (ch !== -1 && ch !== 36 && !(ch >= 40 && ch <= 43) && ch !== 46 && ch !== 63 && ch !== 91 && ch !== 94 && ch !== 124) {
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_groupSpecifier = function(state) {
  if (state.eat(
    63
    /* ? */
  )) {
    if (!this.regexp_eatGroupName(state)) {
      state.raise("Invalid group");
    }
    var trackDisjunction = this.options.ecmaVersion >= 16;
    var known = state.groupNames[state.lastStringValue];
    if (known) {
      if (trackDisjunction) {
        for (var i = 0, list = known; i < list.length; i += 1) {
          var altID = list[i];
          if (!altID.separatedFrom(state.branchID)) {
            state.raise("Duplicate capture group name");
          }
        }
      } else {
        state.raise("Duplicate capture group name");
      }
    }
    if (trackDisjunction) {
      (known || (state.groupNames[state.lastStringValue] = [])).push(state.branchID);
    } else {
      state.groupNames[state.lastStringValue] = true;
    }
  }
};
pp$1.regexp_eatGroupName = function(state) {
  state.lastStringValue = "";
  if (state.eat(
    60
    /* < */
  )) {
    if (this.regexp_eatRegExpIdentifierName(state) && state.eat(
      62
      /* > */
    )) {
      return true;
    }
    state.raise("Invalid capture group name");
  }
  return false;
};
pp$1.regexp_eatRegExpIdentifierName = function(state) {
  state.lastStringValue = "";
  if (this.regexp_eatRegExpIdentifierStart(state)) {
    state.lastStringValue += codePointToString(state.lastIntValue);
    while (this.regexp_eatRegExpIdentifierPart(state)) {
      state.lastStringValue += codePointToString(state.lastIntValue);
    }
    return true;
  }
  return false;
};
pp$1.regexp_eatRegExpIdentifierStart = function(state) {
  var start = state.pos;
  var forceU = this.options.ecmaVersion >= 11;
  var ch = state.current(forceU);
  state.advance(forceU);
  if (ch === 92 && this.regexp_eatRegExpUnicodeEscapeSequence(state, forceU)) {
    ch = state.lastIntValue;
  }
  if (isRegExpIdentifierStart(ch)) {
    state.lastIntValue = ch;
    return true;
  }
  state.pos = start;
  return false;
};
function isRegExpIdentifierStart(ch) {
  return isIdentifierStart(ch, true) || ch === 36 || ch === 95;
}
pp$1.regexp_eatRegExpIdentifierPart = function(state) {
  var start = state.pos;
  var forceU = this.options.ecmaVersion >= 11;
  var ch = state.current(forceU);
  state.advance(forceU);
  if (ch === 92 && this.regexp_eatRegExpUnicodeEscapeSequence(state, forceU)) {
    ch = state.lastIntValue;
  }
  if (isRegExpIdentifierPart(ch)) {
    state.lastIntValue = ch;
    return true;
  }
  state.pos = start;
  return false;
};
function isRegExpIdentifierPart(ch) {
  return isIdentifierChar(ch, true) || ch === 36 || ch === 95 || ch === 8204 || ch === 8205;
}
pp$1.regexp_eatAtomEscape = function(state) {
  if (this.regexp_eatBackReference(state) || this.regexp_eatCharacterClassEscape(state) || this.regexp_eatCharacterEscape(state) || state.switchN && this.regexp_eatKGroupName(state)) {
    return true;
  }
  if (state.switchU) {
    if (state.current() === 99) {
      state.raise("Invalid unicode escape");
    }
    state.raise("Invalid escape");
  }
  return false;
};
pp$1.regexp_eatBackReference = function(state) {
  var start = state.pos;
  if (this.regexp_eatDecimalEscape(state)) {
    var n = state.lastIntValue;
    if (state.switchU) {
      if (n > state.maxBackReference) {
        state.maxBackReference = n;
      }
      return true;
    }
    if (n <= state.numCapturingParens) {
      return true;
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatKGroupName = function(state) {
  if (state.eat(
    107
    /* k */
  )) {
    if (this.regexp_eatGroupName(state)) {
      state.backReferenceNames.push(state.lastStringValue);
      return true;
    }
    state.raise("Invalid named reference");
  }
  return false;
};
pp$1.regexp_eatCharacterEscape = function(state) {
  return this.regexp_eatControlEscape(state) || this.regexp_eatCControlLetter(state) || this.regexp_eatZero(state) || this.regexp_eatHexEscapeSequence(state) || this.regexp_eatRegExpUnicodeEscapeSequence(state, false) || !state.switchU && this.regexp_eatLegacyOctalEscapeSequence(state) || this.regexp_eatIdentityEscape(state);
};
pp$1.regexp_eatCControlLetter = function(state) {
  var start = state.pos;
  if (state.eat(
    99
    /* c */
  )) {
    if (this.regexp_eatControlLetter(state)) {
      return true;
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatZero = function(state) {
  if (state.current() === 48 && !isDecimalDigit(state.lookahead())) {
    state.lastIntValue = 0;
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_eatControlEscape = function(state) {
  var ch = state.current();
  if (ch === 116) {
    state.lastIntValue = 9;
    state.advance();
    return true;
  }
  if (ch === 110) {
    state.lastIntValue = 10;
    state.advance();
    return true;
  }
  if (ch === 118) {
    state.lastIntValue = 11;
    state.advance();
    return true;
  }
  if (ch === 102) {
    state.lastIntValue = 12;
    state.advance();
    return true;
  }
  if (ch === 114) {
    state.lastIntValue = 13;
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_eatControlLetter = function(state) {
  var ch = state.current();
  if (isControlLetter(ch)) {
    state.lastIntValue = ch % 32;
    state.advance();
    return true;
  }
  return false;
};
function isControlLetter(ch) {
  return ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122;
}
pp$1.regexp_eatRegExpUnicodeEscapeSequence = function(state, forceU) {
  if (forceU === void 0) forceU = false;
  var start = state.pos;
  var switchU = forceU || state.switchU;
  if (state.eat(
    117
    /* u */
  )) {
    if (this.regexp_eatFixedHexDigits(state, 4)) {
      var lead = state.lastIntValue;
      if (switchU && lead >= 55296 && lead <= 56319) {
        var leadSurrogateEnd = state.pos;
        if (state.eat(
          92
          /* \ */
        ) && state.eat(
          117
          /* u */
        ) && this.regexp_eatFixedHexDigits(state, 4)) {
          var trail = state.lastIntValue;
          if (trail >= 56320 && trail <= 57343) {
            state.lastIntValue = (lead - 55296) * 1024 + (trail - 56320) + 65536;
            return true;
          }
        }
        state.pos = leadSurrogateEnd;
        state.lastIntValue = lead;
      }
      return true;
    }
    if (switchU && state.eat(
      123
      /* { */
    ) && this.regexp_eatHexDigits(state) && state.eat(
      125
      /* } */
    ) && isValidUnicode(state.lastIntValue)) {
      return true;
    }
    if (switchU) {
      state.raise("Invalid unicode escape");
    }
    state.pos = start;
  }
  return false;
};
function isValidUnicode(ch) {
  return ch >= 0 && ch <= 1114111;
}
pp$1.regexp_eatIdentityEscape = function(state) {
  if (state.switchU) {
    if (this.regexp_eatSyntaxCharacter(state)) {
      return true;
    }
    if (state.eat(
      47
      /* / */
    )) {
      state.lastIntValue = 47;
      return true;
    }
    return false;
  }
  var ch = state.current();
  if (ch !== 99 && (!state.switchN || ch !== 107)) {
    state.lastIntValue = ch;
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_eatDecimalEscape = function(state) {
  state.lastIntValue = 0;
  var ch = state.current();
  if (ch >= 49 && ch <= 57) {
    do {
      state.lastIntValue = 10 * state.lastIntValue + (ch - 48);
      state.advance();
    } while ((ch = state.current()) >= 48 && ch <= 57);
    return true;
  }
  return false;
};
var CharSetNone = 0;
var CharSetOk = 1;
var CharSetString = 2;
pp$1.regexp_eatCharacterClassEscape = function(state) {
  var ch = state.current();
  if (isCharacterClassEscape(ch)) {
    state.lastIntValue = -1;
    state.advance();
    return CharSetOk;
  }
  var negate = false;
  if (state.switchU && this.options.ecmaVersion >= 9 && ((negate = ch === 80) || ch === 112)) {
    state.lastIntValue = -1;
    state.advance();
    var result;
    if (state.eat(
      123
      /* { */
    ) && (result = this.regexp_eatUnicodePropertyValueExpression(state)) && state.eat(
      125
      /* } */
    )) {
      if (negate && result === CharSetString) {
        state.raise("Invalid property name");
      }
      return result;
    }
    state.raise("Invalid property name");
  }
  return CharSetNone;
};
function isCharacterClassEscape(ch) {
  return ch === 100 || ch === 68 || ch === 115 || ch === 83 || ch === 119 || ch === 87;
}
pp$1.regexp_eatUnicodePropertyValueExpression = function(state) {
  var start = state.pos;
  if (this.regexp_eatUnicodePropertyName(state) && state.eat(
    61
    /* = */
  )) {
    var name = state.lastStringValue;
    if (this.regexp_eatUnicodePropertyValue(state)) {
      var value = state.lastStringValue;
      this.regexp_validateUnicodePropertyNameAndValue(state, name, value);
      return CharSetOk;
    }
  }
  state.pos = start;
  if (this.regexp_eatLoneUnicodePropertyNameOrValue(state)) {
    var nameOrValue = state.lastStringValue;
    return this.regexp_validateUnicodePropertyNameOrValue(state, nameOrValue);
  }
  return CharSetNone;
};
pp$1.regexp_validateUnicodePropertyNameAndValue = function(state, name, value) {
  if (!hasOwn(state.unicodeProperties.nonBinary, name)) {
    state.raise("Invalid property name");
  }
  if (!state.unicodeProperties.nonBinary[name].test(value)) {
    state.raise("Invalid property value");
  }
};
pp$1.regexp_validateUnicodePropertyNameOrValue = function(state, nameOrValue) {
  if (state.unicodeProperties.binary.test(nameOrValue)) {
    return CharSetOk;
  }
  if (state.switchV && state.unicodeProperties.binaryOfStrings.test(nameOrValue)) {
    return CharSetString;
  }
  state.raise("Invalid property name");
};
pp$1.regexp_eatUnicodePropertyName = function(state) {
  var ch = 0;
  state.lastStringValue = "";
  while (isUnicodePropertyNameCharacter(ch = state.current())) {
    state.lastStringValue += codePointToString(ch);
    state.advance();
  }
  return state.lastStringValue !== "";
};
function isUnicodePropertyNameCharacter(ch) {
  return isControlLetter(ch) || ch === 95;
}
pp$1.regexp_eatUnicodePropertyValue = function(state) {
  var ch = 0;
  state.lastStringValue = "";
  while (isUnicodePropertyValueCharacter(ch = state.current())) {
    state.lastStringValue += codePointToString(ch);
    state.advance();
  }
  return state.lastStringValue !== "";
};
function isUnicodePropertyValueCharacter(ch) {
  return isUnicodePropertyNameCharacter(ch) || isDecimalDigit(ch);
}
pp$1.regexp_eatLoneUnicodePropertyNameOrValue = function(state) {
  return this.regexp_eatUnicodePropertyValue(state);
};
pp$1.regexp_eatCharacterClass = function(state) {
  if (state.eat(
    91
    /* [ */
  )) {
    var negate = state.eat(
      94
      /* ^ */
    );
    var result = this.regexp_classContents(state);
    if (!state.eat(
      93
      /* ] */
    )) {
      state.raise("Unterminated character class");
    }
    if (negate && result === CharSetString) {
      state.raise("Negated character class may contain strings");
    }
    return true;
  }
  return false;
};
pp$1.regexp_classContents = function(state) {
  if (state.current() === 93) {
    return CharSetOk;
  }
  if (state.switchV) {
    return this.regexp_classSetExpression(state);
  }
  this.regexp_nonEmptyClassRanges(state);
  return CharSetOk;
};
pp$1.regexp_nonEmptyClassRanges = function(state) {
  while (this.regexp_eatClassAtom(state)) {
    var left = state.lastIntValue;
    if (state.eat(
      45
      /* - */
    ) && this.regexp_eatClassAtom(state)) {
      var right = state.lastIntValue;
      if (state.switchU && (left === -1 || right === -1)) {
        state.raise("Invalid character class");
      }
      if (left !== -1 && right !== -1 && left > right) {
        state.raise("Range out of order in character class");
      }
    }
  }
};
pp$1.regexp_eatClassAtom = function(state) {
  var start = state.pos;
  if (state.eat(
    92
    /* \ */
  )) {
    if (this.regexp_eatClassEscape(state)) {
      return true;
    }
    if (state.switchU) {
      var ch$1 = state.current();
      if (ch$1 === 99 || isOctalDigit(ch$1)) {
        state.raise("Invalid class escape");
      }
      state.raise("Invalid escape");
    }
    state.pos = start;
  }
  var ch = state.current();
  if (ch !== 93) {
    state.lastIntValue = ch;
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_eatClassEscape = function(state) {
  var start = state.pos;
  if (state.eat(
    98
    /* b */
  )) {
    state.lastIntValue = 8;
    return true;
  }
  if (state.switchU && state.eat(
    45
    /* - */
  )) {
    state.lastIntValue = 45;
    return true;
  }
  if (!state.switchU && state.eat(
    99
    /* c */
  )) {
    if (this.regexp_eatClassControlLetter(state)) {
      return true;
    }
    state.pos = start;
  }
  return this.regexp_eatCharacterClassEscape(state) || this.regexp_eatCharacterEscape(state);
};
pp$1.regexp_classSetExpression = function(state) {
  var result = CharSetOk, subResult;
  if (this.regexp_eatClassSetRange(state)) ;
  else if (subResult = this.regexp_eatClassSetOperand(state)) {
    if (subResult === CharSetString) {
      result = CharSetString;
    }
    var start = state.pos;
    while (state.eatChars(
      [38, 38]
      /* && */
    )) {
      if (state.current() !== 38 && (subResult = this.regexp_eatClassSetOperand(state))) {
        if (subResult !== CharSetString) {
          result = CharSetOk;
        }
        continue;
      }
      state.raise("Invalid character in character class");
    }
    if (start !== state.pos) {
      return result;
    }
    while (state.eatChars(
      [45, 45]
      /* -- */
    )) {
      if (this.regexp_eatClassSetOperand(state)) {
        continue;
      }
      state.raise("Invalid character in character class");
    }
    if (start !== state.pos) {
      return result;
    }
  } else {
    state.raise("Invalid character in character class");
  }
  for (; ; ) {
    if (this.regexp_eatClassSetRange(state)) {
      continue;
    }
    subResult = this.regexp_eatClassSetOperand(state);
    if (!subResult) {
      return result;
    }
    if (subResult === CharSetString) {
      result = CharSetString;
    }
  }
};
pp$1.regexp_eatClassSetRange = function(state) {
  var start = state.pos;
  if (this.regexp_eatClassSetCharacter(state)) {
    var left = state.lastIntValue;
    if (state.eat(
      45
      /* - */
    ) && this.regexp_eatClassSetCharacter(state)) {
      var right = state.lastIntValue;
      if (left !== -1 && right !== -1 && left > right) {
        state.raise("Range out of order in character class");
      }
      return true;
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatClassSetOperand = function(state) {
  if (this.regexp_eatClassSetCharacter(state)) {
    return CharSetOk;
  }
  return this.regexp_eatClassStringDisjunction(state) || this.regexp_eatNestedClass(state);
};
pp$1.regexp_eatNestedClass = function(state) {
  var start = state.pos;
  if (state.eat(
    91
    /* [ */
  )) {
    var negate = state.eat(
      94
      /* ^ */
    );
    var result = this.regexp_classContents(state);
    if (state.eat(
      93
      /* ] */
    )) {
      if (negate && result === CharSetString) {
        state.raise("Negated character class may contain strings");
      }
      return result;
    }
    state.pos = start;
  }
  if (state.eat(
    92
    /* \ */
  )) {
    var result$1 = this.regexp_eatCharacterClassEscape(state);
    if (result$1) {
      return result$1;
    }
    state.pos = start;
  }
  return null;
};
pp$1.regexp_eatClassStringDisjunction = function(state) {
  var start = state.pos;
  if (state.eatChars(
    [92, 113]
    /* \q */
  )) {
    if (state.eat(
      123
      /* { */
    )) {
      var result = this.regexp_classStringDisjunctionContents(state);
      if (state.eat(
        125
        /* } */
      )) {
        return result;
      }
    } else {
      state.raise("Invalid escape");
    }
    state.pos = start;
  }
  return null;
};
pp$1.regexp_classStringDisjunctionContents = function(state) {
  var result = this.regexp_classString(state);
  while (state.eat(
    124
    /* | */
  )) {
    if (this.regexp_classString(state) === CharSetString) {
      result = CharSetString;
    }
  }
  return result;
};
pp$1.regexp_classString = function(state) {
  var count = 0;
  while (this.regexp_eatClassSetCharacter(state)) {
    count++;
  }
  return count === 1 ? CharSetOk : CharSetString;
};
pp$1.regexp_eatClassSetCharacter = function(state) {
  var start = state.pos;
  if (state.eat(
    92
    /* \ */
  )) {
    if (this.regexp_eatCharacterEscape(state) || this.regexp_eatClassSetReservedPunctuator(state)) {
      return true;
    }
    if (state.eat(
      98
      /* b */
    )) {
      state.lastIntValue = 8;
      return true;
    }
    state.pos = start;
    return false;
  }
  var ch = state.current();
  if (ch < 0 || ch === state.lookahead() && isClassSetReservedDoublePunctuatorCharacter(ch)) {
    return false;
  }
  if (isClassSetSyntaxCharacter(ch)) {
    return false;
  }
  state.advance();
  state.lastIntValue = ch;
  return true;
};
function isClassSetReservedDoublePunctuatorCharacter(ch) {
  return ch === 33 || ch >= 35 && ch <= 38 || ch >= 42 && ch <= 44 || ch === 46 || ch >= 58 && ch <= 64 || ch === 94 || ch === 96 || ch === 126;
}
function isClassSetSyntaxCharacter(ch) {
  return ch === 40 || ch === 41 || ch === 45 || ch === 47 || ch >= 91 && ch <= 93 || ch >= 123 && ch <= 125;
}
pp$1.regexp_eatClassSetReservedPunctuator = function(state) {
  var ch = state.current();
  if (isClassSetReservedPunctuator(ch)) {
    state.lastIntValue = ch;
    state.advance();
    return true;
  }
  return false;
};
function isClassSetReservedPunctuator(ch) {
  return ch === 33 || ch === 35 || ch === 37 || ch === 38 || ch === 44 || ch === 45 || ch >= 58 && ch <= 62 || ch === 64 || ch === 96 || ch === 126;
}
pp$1.regexp_eatClassControlLetter = function(state) {
  var ch = state.current();
  if (isDecimalDigit(ch) || ch === 95) {
    state.lastIntValue = ch % 32;
    state.advance();
    return true;
  }
  return false;
};
pp$1.regexp_eatHexEscapeSequence = function(state) {
  var start = state.pos;
  if (state.eat(
    120
    /* x */
  )) {
    if (this.regexp_eatFixedHexDigits(state, 2)) {
      return true;
    }
    if (state.switchU) {
      state.raise("Invalid escape");
    }
    state.pos = start;
  }
  return false;
};
pp$1.regexp_eatDecimalDigits = function(state) {
  var start = state.pos;
  var ch = 0;
  state.lastIntValue = 0;
  while (isDecimalDigit(ch = state.current())) {
    state.lastIntValue = 10 * state.lastIntValue + (ch - 48);
    state.advance();
  }
  return state.pos !== start;
};
function isDecimalDigit(ch) {
  return ch >= 48 && ch <= 57;
}
pp$1.regexp_eatHexDigits = function(state) {
  var start = state.pos;
  var ch = 0;
  state.lastIntValue = 0;
  while (isHexDigit(ch = state.current())) {
    state.lastIntValue = 16 * state.lastIntValue + hexToInt(ch);
    state.advance();
  }
  return state.pos !== start;
};
function isHexDigit(ch) {
  return ch >= 48 && ch <= 57 || ch >= 65 && ch <= 70 || ch >= 97 && ch <= 102;
}
function hexToInt(ch) {
  if (ch >= 65 && ch <= 70) {
    return 10 + (ch - 65);
  }
  if (ch >= 97 && ch <= 102) {
    return 10 + (ch - 97);
  }
  return ch - 48;
}
pp$1.regexp_eatLegacyOctalEscapeSequence = function(state) {
  if (this.regexp_eatOctalDigit(state)) {
    var n1 = state.lastIntValue;
    if (this.regexp_eatOctalDigit(state)) {
      var n2 = state.lastIntValue;
      if (n1 <= 3 && this.regexp_eatOctalDigit(state)) {
        state.lastIntValue = n1 * 64 + n2 * 8 + state.lastIntValue;
      } else {
        state.lastIntValue = n1 * 8 + n2;
      }
    } else {
      state.lastIntValue = n1;
    }
    return true;
  }
  return false;
};
pp$1.regexp_eatOctalDigit = function(state) {
  var ch = state.current();
  if (isOctalDigit(ch)) {
    state.lastIntValue = ch - 48;
    state.advance();
    return true;
  }
  state.lastIntValue = 0;
  return false;
};
function isOctalDigit(ch) {
  return ch >= 48 && ch <= 55;
}
pp$1.regexp_eatFixedHexDigits = function(state, length) {
  var start = state.pos;
  state.lastIntValue = 0;
  for (var i = 0; i < length; ++i) {
    var ch = state.current();
    if (!isHexDigit(ch)) {
      state.pos = start;
      return false;
    }
    state.lastIntValue = 16 * state.lastIntValue + hexToInt(ch);
    state.advance();
  }
  return true;
};
var Token = function Token2(p) {
  this.type = p.type;
  this.value = p.value;
  this.start = p.start;
  this.end = p.end;
  if (p.options.locations) {
    this.loc = new SourceLocation(p, p.startLoc, p.endLoc);
  }
  if (p.options.ranges) {
    this.range = [p.start, p.end];
  }
};
var pp = Parser.prototype;
pp.next = function(ignoreEscapeSequenceInKeyword) {
  if (!ignoreEscapeSequenceInKeyword && this.type.keyword && this.containsEsc) {
    this.raiseRecoverable(this.start, "Escape sequence in keyword " + this.type.keyword);
  }
  if (this.options.onToken) {
    this.options.onToken(new Token(this));
  }
  this.lastTokEnd = this.end;
  this.lastTokStart = this.start;
  this.lastTokEndLoc = this.endLoc;
  this.lastTokStartLoc = this.startLoc;
  this.nextToken();
};
pp.getToken = function() {
  this.next();
  return new Token(this);
};
if (typeof Symbol !== "undefined") {
  pp[Symbol.iterator] = function() {
    var this$1$1 = this;
    return {
      next: function() {
        var token = this$1$1.getToken();
        return {
          done: token.type === types$1.eof,
          value: token
        };
      }
    };
  };
}
pp.nextToken = function() {
  var curContext = this.curContext();
  if (!curContext || !curContext.preserveSpace) {
    this.skipSpace();
  }
  this.start = this.pos;
  if (this.options.locations) {
    this.startLoc = this.curPosition();
  }
  if (this.pos >= this.input.length) {
    return this.finishToken(types$1.eof);
  }
  if (curContext.override) {
    return curContext.override(this);
  } else {
    this.readToken(this.fullCharCodeAtPos());
  }
};
pp.readToken = function(code) {
  if (isIdentifierStart(code, this.options.ecmaVersion >= 6) || code === 92) {
    return this.readWord();
  }
  return this.getTokenFromCode(code);
};
pp.fullCharCodeAtPos = function() {
  var code = this.input.charCodeAt(this.pos);
  if (code <= 55295 || code >= 56320) {
    return code;
  }
  var next = this.input.charCodeAt(this.pos + 1);
  return next <= 56319 || next >= 57344 ? code : (code << 10) + next - 56613888;
};
pp.skipBlockComment = function() {
  var startLoc = this.options.onComment && this.curPosition();
  var start = this.pos, end = this.input.indexOf("*/", this.pos += 2);
  if (end === -1) {
    this.raise(this.pos - 2, "Unterminated comment");
  }
  this.pos = end + 2;
  if (this.options.locations) {
    for (var nextBreak = void 0, pos = start; (nextBreak = nextLineBreak(this.input, pos, this.pos)) > -1; ) {
      ++this.curLine;
      pos = this.lineStart = nextBreak;
    }
  }
  if (this.options.onComment) {
    this.options.onComment(
      true,
      this.input.slice(start + 2, end),
      start,
      this.pos,
      startLoc,
      this.curPosition()
    );
  }
};
pp.skipLineComment = function(startSkip) {
  var start = this.pos;
  var startLoc = this.options.onComment && this.curPosition();
  var ch = this.input.charCodeAt(this.pos += startSkip);
  while (this.pos < this.input.length && !isNewLine(ch)) {
    ch = this.input.charCodeAt(++this.pos);
  }
  if (this.options.onComment) {
    this.options.onComment(
      false,
      this.input.slice(start + startSkip, this.pos),
      start,
      this.pos,
      startLoc,
      this.curPosition()
    );
  }
};
pp.skipSpace = function() {
  loop: while (this.pos < this.input.length) {
    var ch = this.input.charCodeAt(this.pos);
    switch (ch) {
      case 32:
      case 160:
        ++this.pos;
        break;
      case 13:
        if (this.input.charCodeAt(this.pos + 1) === 10) {
          ++this.pos;
        }
      case 10:
      case 8232:
      case 8233:
        ++this.pos;
        if (this.options.locations) {
          ++this.curLine;
          this.lineStart = this.pos;
        }
        break;
      case 47:
        switch (this.input.charCodeAt(this.pos + 1)) {
          case 42:
            this.skipBlockComment();
            break;
          case 47:
            this.skipLineComment(2);
            break;
          default:
            break loop;
        }
        break;
      default:
        if (ch > 8 && ch < 14 || ch >= 5760 && nonASCIIwhitespace.test(String.fromCharCode(ch))) {
          ++this.pos;
        } else {
          break loop;
        }
    }
  }
};
pp.finishToken = function(type, val) {
  this.end = this.pos;
  if (this.options.locations) {
    this.endLoc = this.curPosition();
  }
  var prevType = this.type;
  this.type = type;
  this.value = val;
  this.updateContext(prevType);
};
pp.readToken_dot = function() {
  var next = this.input.charCodeAt(this.pos + 1);
  if (next >= 48 && next <= 57) {
    return this.readNumber(true);
  }
  var next2 = this.input.charCodeAt(this.pos + 2);
  if (this.options.ecmaVersion >= 6 && next === 46 && next2 === 46) {
    this.pos += 3;
    return this.finishToken(types$1.ellipsis);
  } else {
    ++this.pos;
    return this.finishToken(types$1.dot);
  }
};
pp.readToken_slash = function() {
  var next = this.input.charCodeAt(this.pos + 1);
  if (this.exprAllowed) {
    ++this.pos;
    return this.readRegexp();
  }
  if (next === 61) {
    return this.finishOp(types$1.assign, 2);
  }
  return this.finishOp(types$1.slash, 1);
};
pp.readToken_mult_modulo_exp = function(code) {
  var next = this.input.charCodeAt(this.pos + 1);
  var size = 1;
  var tokentype = code === 42 ? types$1.star : types$1.modulo;
  if (this.options.ecmaVersion >= 7 && code === 42 && next === 42) {
    ++size;
    tokentype = types$1.starstar;
    next = this.input.charCodeAt(this.pos + 2);
  }
  if (next === 61) {
    return this.finishOp(types$1.assign, size + 1);
  }
  return this.finishOp(tokentype, size);
};
pp.readToken_pipe_amp = function(code) {
  var next = this.input.charCodeAt(this.pos + 1);
  if (next === code) {
    if (this.options.ecmaVersion >= 12) {
      var next2 = this.input.charCodeAt(this.pos + 2);
      if (next2 === 61) {
        return this.finishOp(types$1.assign, 3);
      }
    }
    return this.finishOp(code === 124 ? types$1.logicalOR : types$1.logicalAND, 2);
  }
  if (next === 61) {
    return this.finishOp(types$1.assign, 2);
  }
  return this.finishOp(code === 124 ? types$1.bitwiseOR : types$1.bitwiseAND, 1);
};
pp.readToken_caret = function() {
  var next = this.input.charCodeAt(this.pos + 1);
  if (next === 61) {
    return this.finishOp(types$1.assign, 2);
  }
  return this.finishOp(types$1.bitwiseXOR, 1);
};
pp.readToken_plus_min = function(code) {
  var next = this.input.charCodeAt(this.pos + 1);
  if (next === code) {
    if (next === 45 && !this.inModule && this.input.charCodeAt(this.pos + 2) === 62 && (this.lastTokEnd === 0 || lineBreak.test(this.input.slice(this.lastTokEnd, this.pos)))) {
      this.skipLineComment(3);
      this.skipSpace();
      return this.nextToken();
    }
    return this.finishOp(types$1.incDec, 2);
  }
  if (next === 61) {
    return this.finishOp(types$1.assign, 2);
  }
  return this.finishOp(types$1.plusMin, 1);
};
pp.readToken_lt_gt = function(code) {
  var next = this.input.charCodeAt(this.pos + 1);
  var size = 1;
  if (next === code) {
    size = code === 62 && this.input.charCodeAt(this.pos + 2) === 62 ? 3 : 2;
    if (this.input.charCodeAt(this.pos + size) === 61) {
      return this.finishOp(types$1.assign, size + 1);
    }
    return this.finishOp(types$1.bitShift, size);
  }
  if (next === 33 && code === 60 && !this.inModule && this.input.charCodeAt(this.pos + 2) === 45 && this.input.charCodeAt(this.pos + 3) === 45) {
    this.skipLineComment(4);
    this.skipSpace();
    return this.nextToken();
  }
  if (next === 61) {
    size = 2;
  }
  return this.finishOp(types$1.relational, size);
};
pp.readToken_eq_excl = function(code) {
  var next = this.input.charCodeAt(this.pos + 1);
  if (next === 61) {
    return this.finishOp(types$1.equality, this.input.charCodeAt(this.pos + 2) === 61 ? 3 : 2);
  }
  if (code === 61 && next === 62 && this.options.ecmaVersion >= 6) {
    this.pos += 2;
    return this.finishToken(types$1.arrow);
  }
  return this.finishOp(code === 61 ? types$1.eq : types$1.prefix, 1);
};
pp.readToken_question = function() {
  var ecmaVersion = this.options.ecmaVersion;
  if (ecmaVersion >= 11) {
    var next = this.input.charCodeAt(this.pos + 1);
    if (next === 46) {
      var next2 = this.input.charCodeAt(this.pos + 2);
      if (next2 < 48 || next2 > 57) {
        return this.finishOp(types$1.questionDot, 2);
      }
    }
    if (next === 63) {
      if (ecmaVersion >= 12) {
        var next2$1 = this.input.charCodeAt(this.pos + 2);
        if (next2$1 === 61) {
          return this.finishOp(types$1.assign, 3);
        }
      }
      return this.finishOp(types$1.coalesce, 2);
    }
  }
  return this.finishOp(types$1.question, 1);
};
pp.readToken_numberSign = function() {
  var ecmaVersion = this.options.ecmaVersion;
  var code = 35;
  if (ecmaVersion >= 13) {
    ++this.pos;
    code = this.fullCharCodeAtPos();
    if (isIdentifierStart(code, true) || code === 92) {
      return this.finishToken(types$1.privateId, this.readWord1());
    }
  }
  this.raise(this.pos, "Unexpected character '" + codePointToString(code) + "'");
};
pp.getTokenFromCode = function(code) {
  switch (code) {
    // The interpretation of a dot depends on whether it is followed
    // by a digit or another two dots.
    case 46:
      return this.readToken_dot();
    // Punctuation tokens.
    case 40:
      ++this.pos;
      return this.finishToken(types$1.parenL);
    case 41:
      ++this.pos;
      return this.finishToken(types$1.parenR);
    case 59:
      ++this.pos;
      return this.finishToken(types$1.semi);
    case 44:
      ++this.pos;
      return this.finishToken(types$1.comma);
    case 91:
      ++this.pos;
      return this.finishToken(types$1.bracketL);
    case 93:
      ++this.pos;
      return this.finishToken(types$1.bracketR);
    case 123:
      ++this.pos;
      return this.finishToken(types$1.braceL);
    case 125:
      ++this.pos;
      return this.finishToken(types$1.braceR);
    case 58:
      ++this.pos;
      return this.finishToken(types$1.colon);
    case 96:
      if (this.options.ecmaVersion < 6) {
        break;
      }
      ++this.pos;
      return this.finishToken(types$1.backQuote);
    case 48:
      var next = this.input.charCodeAt(this.pos + 1);
      if (next === 120 || next === 88) {
        return this.readRadixNumber(16);
      }
      if (this.options.ecmaVersion >= 6) {
        if (next === 111 || next === 79) {
          return this.readRadixNumber(8);
        }
        if (next === 98 || next === 66) {
          return this.readRadixNumber(2);
        }
      }
    // Anything else beginning with a digit is an integer, octal
    // number, or float.
    case 49:
    case 50:
    case 51:
    case 52:
    case 53:
    case 54:
    case 55:
    case 56:
    case 57:
      return this.readNumber(false);
    // Quotes produce strings.
    case 34:
    case 39:
      return this.readString(code);
    // Operators are parsed inline in tiny state machines. '=' (61) is
    // often referred to. `finishOp` simply skips the amount of
    // characters it is given as second argument, and returns a token
    // of the type given by its first argument.
    case 47:
      return this.readToken_slash();
    case 37:
    case 42:
      return this.readToken_mult_modulo_exp(code);
    case 124:
    case 38:
      return this.readToken_pipe_amp(code);
    case 94:
      return this.readToken_caret();
    case 43:
    case 45:
      return this.readToken_plus_min(code);
    case 60:
    case 62:
      return this.readToken_lt_gt(code);
    case 61:
    case 33:
      return this.readToken_eq_excl(code);
    case 63:
      return this.readToken_question();
    case 126:
      return this.finishOp(types$1.prefix, 1);
    case 35:
      return this.readToken_numberSign();
  }
  this.raise(this.pos, "Unexpected character '" + codePointToString(code) + "'");
};
pp.finishOp = function(type, size) {
  var str = this.input.slice(this.pos, this.pos + size);
  this.pos += size;
  return this.finishToken(type, str);
};
pp.readRegexp = function() {
  var escaped, inClass, start = this.pos;
  for (; ; ) {
    if (this.pos >= this.input.length) {
      this.raise(start, "Unterminated regular expression");
    }
    var ch = this.input.charAt(this.pos);
    if (lineBreak.test(ch)) {
      this.raise(start, "Unterminated regular expression");
    }
    if (!escaped) {
      if (ch === "[") {
        inClass = true;
      } else if (ch === "]" && inClass) {
        inClass = false;
      } else if (ch === "/" && !inClass) {
        break;
      }
      escaped = ch === "\\";
    } else {
      escaped = false;
    }
    ++this.pos;
  }
  var pattern = this.input.slice(start, this.pos);
  ++this.pos;
  var flagsStart = this.pos;
  var flags = this.readWord1();
  if (this.containsEsc) {
    this.unexpected(flagsStart);
  }
  var state = this.regexpState || (this.regexpState = new RegExpValidationState(this));
  state.reset(start, pattern, flags);
  this.validateRegExpFlags(state);
  this.validateRegExpPattern(state);
  var value = null;
  try {
    value = new RegExp(pattern, flags);
  } catch (e) {
  }
  return this.finishToken(types$1.regexp, { pattern, flags, value });
};
pp.readInt = function(radix, len, maybeLegacyOctalNumericLiteral) {
  var allowSeparators = this.options.ecmaVersion >= 12 && len === void 0;
  var isLegacyOctalNumericLiteral = maybeLegacyOctalNumericLiteral && this.input.charCodeAt(this.pos) === 48;
  var start = this.pos, total = 0, lastCode = 0;
  for (var i = 0, e = len == null ? Infinity : len; i < e; ++i, ++this.pos) {
    var code = this.input.charCodeAt(this.pos), val = void 0;
    if (allowSeparators && code === 95) {
      if (isLegacyOctalNumericLiteral) {
        this.raiseRecoverable(this.pos, "Numeric separator is not allowed in legacy octal numeric literals");
      }
      if (lastCode === 95) {
        this.raiseRecoverable(this.pos, "Numeric separator must be exactly one underscore");
      }
      if (i === 0) {
        this.raiseRecoverable(this.pos, "Numeric separator is not allowed at the first of digits");
      }
      lastCode = code;
      continue;
    }
    if (code >= 97) {
      val = code - 97 + 10;
    } else if (code >= 65) {
      val = code - 65 + 10;
    } else if (code >= 48 && code <= 57) {
      val = code - 48;
    } else {
      val = Infinity;
    }
    if (val >= radix) {
      break;
    }
    lastCode = code;
    total = total * radix + val;
  }
  if (allowSeparators && lastCode === 95) {
    this.raiseRecoverable(this.pos - 1, "Numeric separator is not allowed at the last of digits");
  }
  if (this.pos === start || len != null && this.pos - start !== len) {
    return null;
  }
  return total;
};
function stringToNumber(str, isLegacyOctalNumericLiteral) {
  if (isLegacyOctalNumericLiteral) {
    return parseInt(str, 8);
  }
  return parseFloat(str.replace(/_/g, ""));
}
function stringToBigInt(str) {
  if (typeof BigInt !== "function") {
    return null;
  }
  return BigInt(str.replace(/_/g, ""));
}
pp.readRadixNumber = function(radix) {
  var start = this.pos;
  this.pos += 2;
  var val = this.readInt(radix);
  if (val == null) {
    this.raise(this.start + 2, "Expected number in radix " + radix);
  }
  if (this.options.ecmaVersion >= 11 && this.input.charCodeAt(this.pos) === 110) {
    val = stringToBigInt(this.input.slice(start, this.pos));
    ++this.pos;
  } else if (isIdentifierStart(this.fullCharCodeAtPos())) {
    this.raise(this.pos, "Identifier directly after number");
  }
  return this.finishToken(types$1.num, val);
};
pp.readNumber = function(startsWithDot) {
  var start = this.pos;
  if (!startsWithDot && this.readInt(10, void 0, true) === null) {
    this.raise(start, "Invalid number");
  }
  var octal = this.pos - start >= 2 && this.input.charCodeAt(start) === 48;
  if (octal && this.strict) {
    this.raise(start, "Invalid number");
  }
  var next = this.input.charCodeAt(this.pos);
  if (!octal && !startsWithDot && this.options.ecmaVersion >= 11 && next === 110) {
    var val$1 = stringToBigInt(this.input.slice(start, this.pos));
    ++this.pos;
    if (isIdentifierStart(this.fullCharCodeAtPos())) {
      this.raise(this.pos, "Identifier directly after number");
    }
    return this.finishToken(types$1.num, val$1);
  }
  if (octal && /[89]/.test(this.input.slice(start, this.pos))) {
    octal = false;
  }
  if (next === 46 && !octal) {
    ++this.pos;
    this.readInt(10);
    next = this.input.charCodeAt(this.pos);
  }
  if ((next === 69 || next === 101) && !octal) {
    next = this.input.charCodeAt(++this.pos);
    if (next === 43 || next === 45) {
      ++this.pos;
    }
    if (this.readInt(10) === null) {
      this.raise(start, "Invalid number");
    }
  }
  if (isIdentifierStart(this.fullCharCodeAtPos())) {
    this.raise(this.pos, "Identifier directly after number");
  }
  var val = stringToNumber(this.input.slice(start, this.pos), octal);
  return this.finishToken(types$1.num, val);
};
pp.readCodePoint = function() {
  var ch = this.input.charCodeAt(this.pos), code;
  if (ch === 123) {
    if (this.options.ecmaVersion < 6) {
      this.unexpected();
    }
    var codePos = ++this.pos;
    code = this.readHexChar(this.input.indexOf("}", this.pos) - this.pos);
    ++this.pos;
    if (code > 1114111) {
      this.invalidStringToken(codePos, "Code point out of bounds");
    }
  } else {
    code = this.readHexChar(4);
  }
  return code;
};
pp.readString = function(quote) {
  var out2 = "", chunkStart = ++this.pos;
  for (; ; ) {
    if (this.pos >= this.input.length) {
      this.raise(this.start, "Unterminated string constant");
    }
    var ch = this.input.charCodeAt(this.pos);
    if (ch === quote) {
      break;
    }
    if (ch === 92) {
      out2 += this.input.slice(chunkStart, this.pos);
      out2 += this.readEscapedChar(false);
      chunkStart = this.pos;
    } else if (ch === 8232 || ch === 8233) {
      if (this.options.ecmaVersion < 10) {
        this.raise(this.start, "Unterminated string constant");
      }
      ++this.pos;
      if (this.options.locations) {
        this.curLine++;
        this.lineStart = this.pos;
      }
    } else {
      if (isNewLine(ch)) {
        this.raise(this.start, "Unterminated string constant");
      }
      ++this.pos;
    }
  }
  out2 += this.input.slice(chunkStart, this.pos++);
  return this.finishToken(types$1.string, out2);
};
var INVALID_TEMPLATE_ESCAPE_ERROR = {};
pp.tryReadTemplateToken = function() {
  this.inTemplateElement = true;
  try {
    this.readTmplToken();
  } catch (err) {
    if (err === INVALID_TEMPLATE_ESCAPE_ERROR) {
      this.readInvalidTemplateToken();
    } else {
      throw err;
    }
  }
  this.inTemplateElement = false;
};
pp.invalidStringToken = function(position, message) {
  if (this.inTemplateElement && this.options.ecmaVersion >= 9) {
    throw INVALID_TEMPLATE_ESCAPE_ERROR;
  } else {
    this.raise(position, message);
  }
};
pp.readTmplToken = function() {
  var out2 = "", chunkStart = this.pos;
  for (; ; ) {
    if (this.pos >= this.input.length) {
      this.raise(this.start, "Unterminated template");
    }
    var ch = this.input.charCodeAt(this.pos);
    if (ch === 96 || ch === 36 && this.input.charCodeAt(this.pos + 1) === 123) {
      if (this.pos === this.start && (this.type === types$1.template || this.type === types$1.invalidTemplate)) {
        if (ch === 36) {
          this.pos += 2;
          return this.finishToken(types$1.dollarBraceL);
        } else {
          ++this.pos;
          return this.finishToken(types$1.backQuote);
        }
      }
      out2 += this.input.slice(chunkStart, this.pos);
      return this.finishToken(types$1.template, out2);
    }
    if (ch === 92) {
      out2 += this.input.slice(chunkStart, this.pos);
      out2 += this.readEscapedChar(true);
      chunkStart = this.pos;
    } else if (isNewLine(ch)) {
      out2 += this.input.slice(chunkStart, this.pos);
      ++this.pos;
      switch (ch) {
        case 13:
          if (this.input.charCodeAt(this.pos) === 10) {
            ++this.pos;
          }
        case 10:
          out2 += "\n";
          break;
        default:
          out2 += String.fromCharCode(ch);
          break;
      }
      if (this.options.locations) {
        ++this.curLine;
        this.lineStart = this.pos;
      }
      chunkStart = this.pos;
    } else {
      ++this.pos;
    }
  }
};
pp.readInvalidTemplateToken = function() {
  for (; this.pos < this.input.length; this.pos++) {
    switch (this.input[this.pos]) {
      case "\\":
        ++this.pos;
        break;
      case "$":
        if (this.input[this.pos + 1] !== "{") {
          break;
        }
      // fall through
      case "`":
        return this.finishToken(types$1.invalidTemplate, this.input.slice(this.start, this.pos));
      case "\r":
        if (this.input[this.pos + 1] === "\n") {
          ++this.pos;
        }
      // fall through
      case "\n":
      case "\u2028":
      case "\u2029":
        ++this.curLine;
        this.lineStart = this.pos + 1;
        break;
    }
  }
  this.raise(this.start, "Unterminated template");
};
pp.readEscapedChar = function(inTemplate) {
  var ch = this.input.charCodeAt(++this.pos);
  ++this.pos;
  switch (ch) {
    case 110:
      return "\n";
    // 'n' -> '\n'
    case 114:
      return "\r";
    // 'r' -> '\r'
    case 120:
      return String.fromCharCode(this.readHexChar(2));
    // 'x'
    case 117:
      return codePointToString(this.readCodePoint());
    // 'u'
    case 116:
      return "	";
    // 't' -> '\t'
    case 98:
      return "\b";
    // 'b' -> '\b'
    case 118:
      return "\v";
    // 'v' -> '\u000b'
    case 102:
      return "\f";
    // 'f' -> '\f'
    case 13:
      if (this.input.charCodeAt(this.pos) === 10) {
        ++this.pos;
      }
    // '\r\n'
    case 10:
      if (this.options.locations) {
        this.lineStart = this.pos;
        ++this.curLine;
      }
      return "";
    case 56:
    case 57:
      if (this.strict) {
        this.invalidStringToken(
          this.pos - 1,
          "Invalid escape sequence"
        );
      }
      if (inTemplate) {
        var codePos = this.pos - 1;
        this.invalidStringToken(
          codePos,
          "Invalid escape sequence in template string"
        );
      }
    default:
      if (ch >= 48 && ch <= 55) {
        var octalStr = this.input.substr(this.pos - 1, 3).match(/^[0-7]+/)[0];
        var octal = parseInt(octalStr, 8);
        if (octal > 255) {
          octalStr = octalStr.slice(0, -1);
          octal = parseInt(octalStr, 8);
        }
        this.pos += octalStr.length - 1;
        ch = this.input.charCodeAt(this.pos);
        if ((octalStr !== "0" || ch === 56 || ch === 57) && (this.strict || inTemplate)) {
          this.invalidStringToken(
            this.pos - 1 - octalStr.length,
            inTemplate ? "Octal literal in template string" : "Octal literal in strict mode"
          );
        }
        return String.fromCharCode(octal);
      }
      if (isNewLine(ch)) {
        if (this.options.locations) {
          this.lineStart = this.pos;
          ++this.curLine;
        }
        return "";
      }
      return String.fromCharCode(ch);
  }
};
pp.readHexChar = function(len) {
  var codePos = this.pos;
  var n = this.readInt(16, len);
  if (n === null) {
    this.invalidStringToken(codePos, "Bad character escape sequence");
  }
  return n;
};
pp.readWord1 = function() {
  this.containsEsc = false;
  var word = "", first = true, chunkStart = this.pos;
  var astral = this.options.ecmaVersion >= 6;
  while (this.pos < this.input.length) {
    var ch = this.fullCharCodeAtPos();
    if (isIdentifierChar(ch, astral)) {
      this.pos += ch <= 65535 ? 1 : 2;
    } else if (ch === 92) {
      this.containsEsc = true;
      word += this.input.slice(chunkStart, this.pos);
      var escStart = this.pos;
      if (this.input.charCodeAt(++this.pos) !== 117) {
        this.invalidStringToken(this.pos, "Expecting Unicode escape sequence \\uXXXX");
      }
      ++this.pos;
      var esc = this.readCodePoint();
      if (!(first ? isIdentifierStart : isIdentifierChar)(esc, astral)) {
        this.invalidStringToken(escStart, "Invalid Unicode escape");
      }
      word += codePointToString(esc);
      chunkStart = this.pos;
    } else {
      break;
    }
    first = false;
  }
  return word + this.input.slice(chunkStart, this.pos);
};
pp.readWord = function() {
  var word = this.readWord1();
  var type = types$1.name;
  if (this.keywords.test(word)) {
    type = keywords[word];
  }
  return this.finishToken(type, word);
};
var version = "8.14.0";
Parser.acorn = {
  Parser,
  version,
  defaultOptions,
  Position,
  SourceLocation,
  getLineInfo,
  Node,
  TokenType,
  tokTypes: types$1,
  keywordTypes: keywords,
  TokContext,
  tokContexts: types,
  isIdentifierChar,
  isIdentifierStart,
  Token,
  isNewLine,
  lineBreak,
  lineBreakG,
  nonASCIIwhitespace
};
function parse3(input, options) {
  return Parser.parse(input, options);
}

// lint-js.mjs
var import_sucrase = __toESM(require_dist2(), 1);
var file = scriptArgs[1];
var mode = scriptArgs[2];
var src = std.loadFile(file);
var problems = [];
try {
  if (mode === "ts") {
    (0, import_sucrase.transform)(src, { transforms: ["typescript"] });
  } else {
    parse3(src, { ecmaVersion: "latest", sourceType: "script", allowReturnOutsideFunction: false });
  }
} catch (e) {
  const line = e.loc ? e.loc.line : e.lineNumber || 1;
  const col = e.loc ? e.loc.column + 1 : 1;
  problems.push("main." + mode + ":" + line + ":" + col + ": error: " + e.message);
}
if (problems.length === 0) {
  std.out.puts("no issues\n");
} else {
  std.out.puts(problems.join("\n") + "\n");
  std.exit(1);
}
