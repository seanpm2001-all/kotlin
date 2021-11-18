import OC = JS_TESTS.foo.OC;
import AC = JS_TESTS.foo.AC;
import FC = JS_TESTS.foo.FC;
import O1 = JS_TESTS.foo.O1;
import O2 = JS_TESTS.foo.O2;
import getI3 = JS_TESTS.foo.getI3;
import getA = JS_TESTS.foo.getA;
import getB = JS_TESTS.foo.getB;
import getC = JS_TESTS.foo.getC;

class Impl extends AC {
    z(z: number): void {
    }

    get acAbstractProp(): string { return "Impl"; }
    get y(): boolean { return true; }
}

function box(): string {
    const impl = new Impl();
    if (impl.acProp !== "acProp") return "Fail 1";
    if (impl.x !== "AC") return "Fail 2";
    if (impl.acAbstractProp !== "Impl") return "Fail 2.1";
    if (impl.y !== true) return "Fail 2.2";


    const oc = new OC(false, "OC");
    if (oc.y !== false) return "Fail 3";
    if (oc.acAbstractProp !== "OC") return "Fail 4";
    oc.z(10);


    const fc = new FC();
    if (fc.y !== true) return "Fail 5";
    if (fc.acAbstractProp !== "FC") return "Fail 6";
    fc.z(10);

    if (O1.y !== true || O2.y !== true) return "Fail 7";
    if (O1.acAbstractProp != "O1") return "Fail 8";
    if (O2.acAbstractProp != "O2") return "Fail 9";
    if (O2.foo() != 10) return "Fail 10";

    if (getI3().foo != "fooI3") return "Fail 11"
    if (getI3().bar != "barI3") return "Fail 12"
    if (getI3().baz != "bazI3") return "Fail 13"

    if (getA().foo != "fooA") return "Fail 14"
    if (getA().bar != "barA") return "Fail 15"
    if (getA().baz != "bazA") return "Fail 16"

    if (getB().foo != "fooB") return "Fail 17"
    if (getB().bar != "barB") return "Fail 18"
    if (getB().baz != "bazB") return "Fail 19"

    if (getC().foo != "fooC") return "Fail 20"
    if (getC().bar != "barC") return "Fail 21"
    if (getC().baz != "bazC") return "Fail 22"

    return "OK";
}