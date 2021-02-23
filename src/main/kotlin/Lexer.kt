enum class TK {
    ERR, EOF, CHAR,
    XVAR, XUSER, XNAT, XNUM, XEMPTY,
    UNIT, ARROW,
    BREAK, CALL, ELSE, FUNC, IF, LOOP, NAT, RET, SET, TYPE, VAR
}

val key2tk: HashMap<String, TK> = hashMapOf (
    "break"  to TK.BREAK,
    "call"   to TK.CALL,
    "else"   to TK.ELSE,
    "func"   to TK.FUNC,
    "if"     to TK.IF,
    "loop"   to TK.LOOP,
    "native" to TK.NAT,
    "return" to TK.RET,
    "set"    to TK.SET,
    "type"   to TK.TYPE,
    "var"    to TK.VAR,
)

sealed class TK_Val
data class TK_Chr (val v: Char):   TK_Val()
data class TK_Str (val v: String): TK_Val()
data class TK_Num (val v: Int):    TK_Val()

data class Tk (
    var enu: TK,
    var pay: TK_Val?,
    var lin: Int,
    var col: Int,
)

fun TK.toErr (chr: Char?): String {
    return when (this) {
        TK.EOF   -> "end of file"
        TK.CHAR  -> "`" + chr!! + "´"
        TK.XVAR  -> "variable identifier"
        TK.XUSER -> "type identifier"
        else -> { println(this) ; error("TODO") }
    }
}

fun Tk.toPay (): String {
    return when (this.enu) {
        TK.EOF   -> "end of file"
        TK.CHAR  -> "`" + (this.pay as TK_Chr).v + "´"
        TK.XVAR  -> '"' + (this.pay as TK_Str).v + '"'
        TK.XUSER -> '"' + (this.pay as TK_Str).v + '"'
        else -> {
            val keys = key2tk.filterValues { it == this.enu }.keys
            if (keys.isEmpty()) {
                println(this); error("TODO")
            } else {
                keys.first()    // TYPE -> "type"
            }
        }
    }
}

fun blanks (all: All) {
    while (true) {
        val (c1,x1) = all.read()
        when (x1) {
            '\n', ' ' -> { }                // ignore line/space
            '-' -> {
                val (c2,x2) = all.read()
                if (x2 == '-') {            // ignore comments
                    while (true) {
                        val (c3,x3) = all.read()
                        if (c3 == -1) {     // EOF stops comment
                            break
                        }
                        if (x3 == '\n') {   // LN stops comment
                            all.unread(c3)
                            break
                        }
                    }
                } else {
                    all.unread(c2)
                    all.unread(c1)
                    return
                }
            }
            else -> {
                all.unread(c1)
                return
            }
        }
    }
}

fun token (all: All) {
    all.tk1.lin = all.lin
    all.tk1.col = all.col

    var (c1,x1) = all.read()
    if (c1 == -1) {
        all.tk1.enu = TK.EOF
    } else {
        when (x1) {
            '{' , '}' , ')' , ';' , ':' , '=' , ',' , '.' , '\\' , '!' , '?' -> {
                all.tk1.enu = TK.CHAR
                all.tk1.pay = TK_Chr(x1)
            }
            '(' -> {
                val (c2,x2) = all.read()
                if (x2 == ')') {
                    all.tk1.enu = TK.UNIT
                } else {
                    all.tk1.enu = TK.CHAR
                    all.tk1.pay = TK_Chr(x1)
                    all.unread(c2)
                }
            }
            '-' -> {
                val (_,x2) = all.read()
                if (x2 == '>') {
                    all.tk1.enu = TK.ARROW
                } else {
                    error("TODO")
                }
            }
            '_' -> {
                var (c2,x2) = all.read()
                var pay = ""

                var open:  Char? = null
                var close: Char? = null
                var open_close = 0
                if (x2=='(' || x2=='{') {
                    open  = x2
                    close = if (x2=='(') ')' else '}'
                    open_close += 1
                    all.read().let { c2=it.first ; x2=it.second }
                }

                while (close!=null || x2.isLetterOrDigit() || x2=='_') {
                    if (x2 == open) {
                        open_close += 1
                    } else if (x2 == close) {
                        open_close -= 1
                        if (open_close == 0) {
                            break
                        }
                    }
                    pay += x2
                    all.read().let { c2=it.first ; x2=it.second }
                }
                if (close == null) {
                    all.unread(c2)
                }
                all.tk1.pay = TK_Str(pay)
                all.tk1.enu = TK.XNAT
            }
            else -> {
                var pay = ""

                val isdollar = (x1 == '$')
                if (isdollar) {
                    all.read().let { c1=it.first ; x1=it.second }
                }

                if (!isdollar && x1.isDigit()) {
                    while (x1.isLetterOrDigit()) {
                        if (x1.isDigit()) {
                            pay += x1
                            all.read().let { c1=it.first ; x1=it.second }
                        } else {
                            all.tk1.enu = TK.ERR
                            all.tk1.pay = TK_Str(pay)
                            return
                        }
                    }
                    all.unread(c1)
                    all.tk1.enu = TK.XNUM
                    all.tk1.pay = TK_Num(pay.toInt())
                } else if (x1.isUpperCase() || (x1.isLowerCase() && !isdollar)) {
                    while (x1.isLetterOrDigit() || x1=='_') {
                        pay += x1
                        all.read().let { c1=it.first ; x1=it.second }
                    }
                    all.unread(c1)
                    all.tk1.pay = TK_Str(pay)
                    val key = key2tk[pay]
                    all.tk1.enu = when {
                        key != null -> { assert(pay[0].isLowerCase()); key }
                        isdollar -> TK.XEMPTY
                        pay[0].isLowerCase() -> TK.XVAR
                        pay[0].isUpperCase() -> TK.XUSER
                        else -> error("impossible case")
                    }
                } else {
                    all.tk1.enu = TK.ERR
                    all.tk1.pay = TK_Chr(x1)
                    return
                }
            }
        }
    }
}

fun lexer (all: All) {
    all.tk0 = all.tk1.copy()
    blanks(all)
    token(all)
    blanks(all)
}