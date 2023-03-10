package quantify

import gossamer.*
import rudiments.*

import scala.quoted.*
import annotation.{targetName, allowConversions}

import language.implicitConversions

trait Dimension

erased trait Length extends Dimension
erased trait Mass extends Dimension
erased trait TimeLength extends Dimension
erased trait Current extends Dimension
erased trait Luminosity extends Dimension
erased trait Temperature extends Dimension
erased trait AmountOfSubstance extends Dimension

trait Units[PowerType <: Int & Singleton, DimensionType <: Dimension]

erased trait Metre[Power <: Int & Singleton] extends Units[Power, Length]
erased trait Kilogram[Power <: Int & Singleton] extends Units[Power, Mass]
erased trait Candela[Power <: Int & Singleton] extends Units[Power, Luminosity]
erased trait Mole[Power <: Int & Singleton] extends Units[Power, AmountOfSubstance]
erased trait Ampere[Power <: Int & Singleton] extends Units[Power, Current]
erased trait Kelvin[Power <: Int & Singleton] extends Units[Power, Temperature]
erased trait Second[Power <: Int & Singleton] extends Units[Power, TimeLength]

val Metre = Quantity[Metre[1]](1)
val Kilogram = Quantity[Kilogram[1]](1)
val Candela = Quantity[Candela[1]](1)
val Mole = Quantity[Mole[1]](1)
val Ampere = Quantity[Ampere[1]](1)
val Kelvin = Quantity[Kelvin[1]](1)
val Second = Quantity[Second[1]](1)

trait UnitName[-ValueType]:
  def name(): Text

object UnitName:
  given UnitName[Metre[1]] = () => t"m"
  given UnitName[Kilogram[1]] = () => t"kg"
  given UnitName[Candela[1]] = () => t"cd"
  given UnitName[Mole[1]] = () => t"mol"
  given UnitName[Ampere[1]] = () => t"A"
  given UnitName[Kelvin[1]] = () => t"K"
  given UnitName[Second[1]] = () => t"s"

object Coefficient:
  given Coefficient[Metre[1], Inch[1]](39.3701)
  given Coefficient[Inch[1], Metre[1]](1.0/39.3701)

trait Coefficient[FromType <: Units[1, ?], ToType <: Units[1, ?]](val value: Double)

trait PrincipalUnit[DimensionType <: Dimension, UnitType <: Units[1, DimensionType]]()
object PrincipalUnit:
  given PrincipalUnit[Length, Metre[1]]()
  given PrincipalUnit[Mass, Kilogram[1]]()
  given PrincipalUnit[TimeLength, Second[1]]()
  given PrincipalUnit[Current, Ampere[1]]()
  given PrincipalUnit[Luminosity, Candela[1]]()
  given PrincipalUnit[Temperature, Kelvin[1]]()
  given PrincipalUnit[AmountOfSubstance, Mole[1]]()

object QuantityOpaques:
  opaque type Quantity[UnitsType <: Units[?, ?]] = Double

  extension [UnitsType <: Units[?, ?]](quantity: Quantity[UnitsType])
    def value: Double = quantity

  object Quantity:
    def apply[UnitsType <: Units[?, ?]](value: Double): Quantity[UnitsType] = value
    
    given convertDouble[UnitsType <: Units[?, ?]]: Conversion[Double, Quantity[UnitsType]] = Quantity(_)
    given convertInt[UnitsType <: Units[?, ?]]: Conversion[Int, Quantity[UnitsType]] = int => Quantity(int.toDouble)

    inline given [UnitsType <: Units[?, ?]](using DecimalFormat): Show[Quantity[UnitsType]] =
      new Show[Quantity[UnitsType]]:
        def show(value: Quantity[UnitsType]): Text = value.render
  
    def renderUnits(units: Map[Text, Int]): Text =
      units.to(List).map: (unit, power) =>
        if power == 1 then unit
        else 
          val exponent: Text =
            power.show.mapChars:
              case '0' => '???'
              case '1' => '??'
              case '2' => '??'
              case '3' => '??'
              case '4' => '???'
              case '5' => '???'
              case '6' => '???'
              case '7' => '???'
              case '8' => '???'
              case '9' => '???'
              case '-' => '???'
              case _   => ' '
          
          t"$unit$exponent"
      .join(t"??")


export QuantityOpaques.Quantity

// class Quantity[UnitsType <: Units[?, ?]](val value: Double):
//   quantity =>

extension [UnitsType <: Units[?, ?]](inline quantity: Quantity[UnitsType])
  @targetName("plus")
  inline def +(quantity2: Quantity[UnitsType]): Quantity[UnitsType] = Quantity(quantity.value + quantity2.value)
  
  @targetName("minus")
  inline def -(quantity2: Quantity[UnitsType]): Quantity[UnitsType] = Quantity(quantity.value - quantity2.value)
  
  @targetName("times2")
  transparent inline def *
      [UnitsType2 <: Units[?, ?]](@allowConversions inline quantity2: Quantity[UnitsType2]): Any =
    ${QuantifyMacros.multiply[UnitsType, UnitsType2]('quantity, 'quantity2, false)}
  
  @targetName("divide2")
  transparent inline def /
      [UnitsType2 <: Units[?, ?]](@allowConversions inline quantity2: Quantity[UnitsType2]): Any =
    ${QuantifyMacros.multiply[UnitsType, UnitsType2]('quantity, 'quantity2, true)}

  inline def units: Map[Text, Int] = ${QuantifyMacros.collectUnits[UnitsType]}
  inline def render(using DecimalFormat): Text = t"${quantity.value}${Quantity.renderUnits(units)}"

extension (value: Double)
  @targetName("times")
  def *[UnitsType <: Units[?, ?]](quantity: Quantity[UnitsType]): Quantity[UnitsType] = quantity*value
  
  // @tarhgetName("divide")
  // def /[UnitsType <: Units[?, ?]](quantity: Quantity[UnitsType]): Quantity[UnitsType] = quantity.invert*value
  