package me.ztiany.kotlin.oop

/**
 * 多态：熟悉 Java 的开发者对多态应该不会陌生，它是面向对象程序设计（OOP）的一个重要特征。
 *
 * - 当我们用一个子类继承一个父类的时候，这就是子类型多态（Subtypepolymorphism）。
 * - 另一种熟悉的多态是参数多态（Parametricpolymorphism），泛型就是其最常见的形式。
 * - 此外，也许你还会想到 C++ 中的运算符重载，我们可以用特设多态（Ad-hocpolymorphism）来描述它。
 *
 * 相比子类型多态和参数多态，可能开发者对特设多态会感到有些许陌生。其实这是一种更加灵活的多态技术，在 Kotlin中 ，一些有趣的语言特性，
 * 如运算符重载、扩展都很好地支持这种多态。
 */
fun main() {

}

///////////////////////////////////////////////////////////////////////////
// 子类型多态：其实就是集成
///////////////////////////////////////////////////////////////////////////
private open class Parent
private class Child : Parent()

///////////////////////////////////////////////////////////////////////////
// 参数多态
///////////////////////////////////////////////////////////////////////////

/**
 * 参数多态在程序设计语言与类型论中是指声明与定义函数、复合类型、变量时不指定其具体的类型，而把这部分类型作为参数使用，
 * 使得该定义对各种具体类型都适用，所以它建立在运行时的参数基础上，并且所有这些都是在不影响类型安全的前提下进行的。
 */
private interface Key {
    val uniqueKey: String
}

fun <T : Key> persist(data: T) {

}

///////////////////////////////////////////////////////////////////////////
// 对第三方类进行扩展
///////////////////////////////////////////////////////////////////////////

/**
 * 假使当对应的业务类 Parent、Child 是第三方引入的，且不可被修改时，如果我们要想给它们扩展一些方法，比如将对象转化为 Json，利用
 * 上面介绍的多态技术就会显得比较麻烦。不过 Kotlin 支持扩展的语法，利用扩展我们就能给 Parent、Child 添加方法或属性，从而换一种思
 * 路来解决上面的问题。
 *
 * 如下，为 Parent 扩展了一个将对象转换为 Json 的 toJson 方法。需要注意的是，扩展属性和方法的实现运行在 Parent 实例，它们的定义操作
 * 并不会修改 ClassA 类本身。这样就为我们带来了一个很大的好处，即被扩展的第三方类免于被污染，从而避免了一些因父类修改而可能导致子类
 * 出错的问题发生。
 *
 * 虽然在 Java 中我们可以依靠其他的办法比如设计模式来解决，但相较而言依靠扩展的方案显得更加方便且合理，这其实也是另一种被称为特设多态的技术。
 */
private fun Parent.toJson(): String {
    return ""
}

///////////////////////////////////////////////////////////////////////////
// 特设多态与运算符重载
///////////////////////////////////////////////////////////////////////////

/**
 * 当你想定义一个通用的 sum 方法时，也许会在 Kotlin 中这么写：
 *
 * ```
 * fun <T> sum(x:T, y:T):T = x + y
 * ```
 *
 * 但编译器会报错，因为某些类型 T 的实例不一定支持加法操作，而且如果针对一些自定义类，我们更希望能够实现各自定制化的“加法语义上的操作”。
 * 如果把参数多态做的事情打个比方：它提供了一个工具，只要一个东西能“切”，就用这个工具来切割它。然而，现实中不是所有的东西都能被切，
 * 而且材料也不一定相同。更加合理的方案是，你可以根据不同的原材料来选择不同的工具来切它。
 *
 * 也许，我们可以定义一个通用的 Summable 接口，然后让需要支持加法操作的类来实现它的 plusThat 方法，一般情况下这没有问题，然而，如果我们
 * 要针对不可修改的第三方类扩展加法操作时，这种通过子类型多态的技术手段也会遇到问题。
 *
 * 此时想到了 Kotlin 的扩展，我们要引出另一种叫作“特设多态”的技术了。相比更通用的参数多态，特设多态提供了“量身定制”的能力。参考它的定义，
 * 特设多态可以理解为：一个多态函数是有多个不同的实现，依赖于其实参而调用相应版本的函数。
 *
 */
private data class Area(val value: Double)

private operator fun Area.plus(that: Area): Area {
    return Area(this.value + that.value)
}

// 操作符重载:将一个函数标记为重载一个操作符或者实现一个约定。