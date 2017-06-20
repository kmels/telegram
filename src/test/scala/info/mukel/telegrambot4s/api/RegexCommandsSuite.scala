package info.mukel.telegrambot4s.api

import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.declarative.{InlineQueries, Messages}
import info.mukel.telegrambot4s.models.{Chat, ChatType, Message}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class RegexCommandsSuite extends FlatSpec with MockFactory {

  def msg(text: String): Message =
    Message(0, chat = Chat(0, ChatType.Private), date = 0, text = text)

  trait Fixture {
    val handler = mockFunction[Message, Seq[String], Unit]
    val bot = new BotBase with Messages with InlineQueries {
      val client: RequestHandler = null
      val logger = Logger(getClass)
      def token = "token"
    }
  }

  behavior of "RegexCommands"

  it should "match simple regex" in new Fixture {
    handler.expects(*, Seq("/pepe")).once()
    handler.expects(*, Seq("/cojo")).once()

    bot.onRegex("""(/pepe|/cojo)""".r)(handler.curried)

    // Invalid
    bot.receiveMessage(msg("/cocou"))
    bot.receiveMessage(msg("/pepecojo"))
    bot.receiveMessage(msg("/cojopepe"))

    // Valid
    bot.receiveMessage(msg("  /pepe  "))
    bot.receiveMessage(msg("/cojo"))
  }

  it should "pass regex groups" in new Fixture {
    handler.expects(*, Seq("1234")).once()
    bot.onRegex("""/cmd ([0-9]+)""".r)(handler.curried)
    bot.receiveMessage(msg("/cmd 1234"))
  }
}
