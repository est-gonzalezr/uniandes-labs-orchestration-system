/** The ExchangeType enum provides the exchange types for the messaging system.
  *
  * @author
  *   Esteban Gonzalez Ruales
  */

package types

/** The ExchangeType enum represents the exchange types for the messaging
  * system.
  */
enum ExchangeType:
  case Direct
  case Fanout
  case Topic
  case Headers
