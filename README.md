# JMac
JMac, or Java-Macro, is a project to try and bring [Rust's `macro_rules!`](https://doc.rust-lang.org/rust-by-example/macros.html) to Java.

## How does it work
When you have JMac added as a [Gradle](https://gradle.org/) plugin and configured, and invoke the `jar` task, JMac's preprocessor will run and as the name implies, preprocess your Java source files. Much like the [C Preprocessor](https://en.wikipedia.org/wiki/C_preprocessor).
JMac will first find your macro definitions and parse them. It will then go through your source files and substitute macro invocations accordingly. JMac does not alter your original source files, but rather tells gradle to compile from a different set of sourcefiles, which are the preprocessed source files. After the preprocessing, the definition classes are removed, as is the `import` to them so that your program will compile as if the macro's were never there.

**This project is still in early development and does not yet work!**

## Contributing
All contributions are welcome! Feel free to fork this repository and open a PR with changes.

## Licence
JMac is dual licenced under the MIT and Apache-2 licence [like the Rust project](https://www.rust-lang.org/policies/licenses) at your descretion.