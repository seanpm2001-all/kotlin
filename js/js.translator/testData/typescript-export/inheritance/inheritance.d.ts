declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {
        interface I<T, S, U> {
            x: T;
            readonly y: S;
            z(u: U): void;
        }
        interface I2 {
            x: string;
            readonly y: boolean;
            z(z: number): void;
        }
    }
    namespace foo {
        abstract class AC implements foo.I2 {
            constructor();
            x: string;
            abstract readonly y: boolean;
            abstract z(z: number): void;
            readonly acProp: string;
            abstract readonly acAbstractProp: string;
        }
        class OC extends foo.AC implements foo.I<string, boolean, number> {
            constructor(y: boolean, acAbstractProp: string);
            readonly y: boolean;
            readonly acAbstractProp: string;
            z(z: number): void;
        }
        class FC extends foo.OC {
            constructor();
        }
        const O1: {
        } & foo.OC;
        const O2: {
            foo(): number;
        } & foo.OC;
        interface I3 {
            readonly foo: string;
            bar: string;
            readonly baz: string;
            bay(): string;
            readonly __doNotUseIt: __doNotImplementIt;
        }
        function getI3(): foo.I3;
        function getA(): foo.I3;
        function getB(): foo.I3;
        function getC(): foo.I3;
        abstract class Aboba2 implements foo.I3 {
            constructor();
            abstract readonly foo: string;
            abstract bar: string;
            abstract readonly baz: string;
            readonly __doNotUseIt: __doNotImplementIt;
        }
        class B2 extends foo.Aboba2 {
            constructor();
            readonly foo: string;
            bar: string;
            readonly baz: string;
            bay(): string;
        }
        class C2 extends foo.B2 {
            constructor();
            readonly foo: string;
            bar: string;
            baz: string;
            bay(): string;
        }
    }
}
