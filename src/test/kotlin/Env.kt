import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@TestMethodOrder(Alphanumeric::class)
class Env {

    fun all (inp: String): String {
        val (ok1,out1) = All_inp2c(inp)
        if (!ok1) {
            return out1
        }
        return "OK"
    }

    // UNDECLARED

    @Test
    fun a01_undeclared_var () {
        val out = all("output std x")
        assert(out == "(ln 1, col 12): undeclared variable \"x\"")
    }
    @Test
    fun a02_undeclared_func () {
        val out = all("call f ()")
        assert(out == "(ln 1, col 6): undeclared variable \"f\"")
    }
    @Test
    fun a03_undeclared_type () {
        val out = all("var x: Nat = ()")
        assert(out == "(ln 1, col 8): undeclared type \"Nat\"")
    }

    // USER

    @Test
    fun b01_user_sup_undeclared () {
        val out = all("""
            var x: Bool = ()
        """.trimIndent())
        assert(out == "(ln 1, col 8): undeclared type \"Bool\"")
    }
    @Test
    fun b02_user_sub_undeclared () {
        val out = all("""
            type Set {
                X: ()
            }
            output std(Set.Set)
        """.trimIndent())
        assert(out == "(ln 4, col 15): undeclared subcase \"Set\"")
    }
    @Test
    fun b03_user_pred_err () {
        val out = all("""
            type Bool { False: () ; True: () }
            type Z { Y:() }
            var z: Z = Z.Y
            output std z.Z?
        """.trimIndent())
        assert(out == "(ln 4, col 14): invalid `.´ : undeclared subcase \"Z\"")
    }
    @Test
    fun b04_user_disc_cons_err () {
        val out = all("""
            output std ().Z!
        """.trimIndent())
        assert(out == "(ln 1, col 12): invalid `.´ : expected user type")
    }
    @Test
    fun b06_user_norec_err () {
        val out = all("""
            type @rec NoRec { X: () ; Y: () }
        """.trimIndent())
        assert(out == "(ln 1, col 11): invalid type declaration : unexpected `@rec´")
    }
    @Test
    fun b07_user_rec_err () {
        val out = all("""
            type Rec { X: Rec ; Y: () }
        """.trimIndent())
        assert(out == "(ln 1, col 15): undeclared type \"Rec\"")
    }
    @Test
    fun b08_user_rec_err () {
        val out = all("""
            type @rec Rec1 { X: Rec1 ; Y: () }
            type Rec2 { X: Rec1 ; Y: () }
        """.trimIndent())
        assert(out == "(ln 2, col 6): invalid type declaration : expected `@rec´")
    }
    @Test
    fun b09_user_err () {
        val Z = "\$Z"
        val out = all("""
            type Z { Z:() }
            type @rec List {
                Item: List
            }
            var l: List = Z.Z
        """.trimIndent())
        assert(out == "(ln 5, col 5): invalid assignment to \"l\" : type mismatch")
    }
    @Test
    fun b10_user_empty_err () {
        val Z = "\$Z"
        val out = all("""
            type Z { Z:() }
            type @rec List {
                Item: List
            }
            var l: List = List.Item Z.Z
        """.trimIndent())
        assert(out == "(ln 5, col 20): invalid constructor \"Item\" : type mismatch")
    }
    @Test
    fun b11_user_empty_err () {
        val out = all("""
            type Z { Z:() }
            type @rec List {
                Item: List
            }
            var l: List = List.Item List.Nil
            output std \l.Z!
        """.trimIndent())
        assert(out == "(ln 6, col 15): invalid `.´ : undeclared subcase \"Z\"")
    }
    @Test
    fun b12_user_empty_ok () {
        val out = all("""
            type Z { Z:() }
            type @rec List {
                Item: List
            }
            var l: List = List.Item List.Nil
            output std \l.Nil!
        """.trimIndent())
        assert(out == "OK")
    }

    // TODO: test if empty is part of isrec
    // TODO: invalid type declaration : unmatching predeclaration

    // TYPE

    @Test
    fun c01_type_var () {
        val out = all("""
            var x: Int = ()
        """.trimIndent())
        assert(out == "(ln 1, col 5): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c02_type_set () {
        val out = all("""
            var x: () = ()
            set x = 10
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c03_type_func_ret () {
        val out = all("""
            func f : () -> () { return 10 }
        """.trimIndent())
        assert(out == "(ln 1, col 21): invalid return : type mismatch")
    }
    @Test
    fun c04_type_func_arg () {
        val out = all("""
            func f : ((),()) -> () { }
            call f()
        """.trimIndent())
        assert(out == "(ln 2, col 6): invalid call to \"f\" : type mismatch")
    }
    @Test
    fun c05_type_idx () {
        val out = all("""
            var x: () = (1,2).1
        """.trimIndent())
        assert(out == "(ln 1, col 5): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c06_type_idx () {
        val out = all("""
            var x: (Int,Int) = (1,2)
            set x.1 = ()
        """.trimIndent())
        assert(out == "(ln 2, col 9): invalid assignment : type mismatch")
    }
    @Test
    fun c07_type_upref () {
        val out = all("""
            var x: \Int = 10
        """.trimIndent())
        assert(out == "(ln 1, col 5): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c08_type_upref () {
        val out = all("""
            var y: Int = 10
            var x: Int = \y
        """.trimIndent())
        assert(out == "(ln 2, col 5): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c09_type_upref () {
        val out = all("""
            var y: Int = 10
            var x: \Int = \y
        """.trimIndent())
        assert(out == "OK")
    }
    @Test
    fun c10_type_upref () {
        val out = all("""
            var y: () = ()
            var x: \Int = \y
        """.trimIndent())
        assert(out == "(ln 2, col 5): invalid assignment to \"x\" : type mismatch")
    }
    @Test
    fun c11_type_upref () {
        val out = all("""
            var y: Int = 10
            var x: \Int = \y
            var z: _x = \x
        """.trimIndent())
        assert(out == "(ln 3, col 14): invalid `\\` : unexpected pointer type")
    }
    @Test
    fun c12_type_dnref () {
        val out = all("""
            var x: Int = 10
            output std x\
        """.trimIndent())
        assert(out == "(ln 2, col 13): invalid `\\` : expected pointer type")
    }
    @Test
    fun c13_type_dnref () {
        val out = all("""
            var x: Int = 10
            var y: \Int = \x
            var z: \Int = y\
        """.trimIndent())
        assert(out == "(ln 3, col 5): invalid assignment to \"z\" : type mismatch")
    }
}