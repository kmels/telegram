package info.mukel.telegrambot4s.api.declarative

import info.mukel.telegrambot4s.api.BotBase
import info.mukel.telegrambot4s.methods.AnswerInlineQuery
import info.mukel.telegrambot4s.models.{InlineQuery, InlineQueryResult}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.matching.Regex

/**
  * Declarative interface for processing inline queries.
  */
trait InlineQueries extends BotBase {

  private val inlineQueryActions = mutable.ArrayBuffer[InlineQueryAction]()

  /** Generic filtering for inline queries.
    *
    * @param filter A filter should not have side effects, and should be fast (no DB requests).
    * @param action Method to process the filtered inline query.
    */
  def whenInlineQuery(filter: InlineQueryFilter)(action: InlineQueryAction): Unit = {
    inlineQueryActions += wrapFilteredAction(filter, action)
  }

  /**
    * Executes 'action' for every inline query.
    */
  def onInlineQuery(action: InlineQueryAction): Unit = {
    inlineQueryActions += action
  }

  /**
    * Filter inline queries messages using a regular expression.
    * Captured groups are passed along with the message to the handler.
    * The query's is trimmed before applying the regex, no need to take care of leading/trailing spaces.
    *
    * '''Warning:'''
    *   Absent optional groups won't be ignored, `null` will be passed instead.
    */
  def onRegexInline(r: Regex)(actionWithArgs: InlineQueryActionWithArgs): Unit = {
    onInlineQuery { iq =>
      iq.query.trim match {
        case r(args @ _*) => actionWithArgs(iq)(args)
        case _ =>
      }
    }
  }

  abstract override def receiveInlineQuery(inlineQuery: InlineQuery): Unit = {
    for (action <- inlineQueryActions)
      action(inlineQuery)

    // Preserve trait stack-ability.
    super.receiveInlineQuery(inlineQuery)
  }

  /** Use this method to send answers to an inline query. On success, True is returned.
    * No more than 50 results per query are allowed.
    *
    * @param results            Array of InlineQueryResult A JSON-serialized array of results for the inline query
    * @param cacheTime          Integer Optional The maximum amount of time in seconds that the result of the inline query may be cached on the server. Defaults to 300.
    * @param isPersonal         Boolean Optional Pass True, if results may be cached on the server side only for the user that sent the query. By default, results may be returned to any user who sends the same query
    * @param nextOffset         String Optional Pass the offset that a client should send in the next query with the same text to receive more results. Pass an empty string if there are no more results or if you don't support pagination. Offset length can't exceed 64 bytes.
    * @param switchPmText       String Optional If passed, clients will display a button with specified text that switches the user to a private chat with the bot and sends the bot a start message with the parameter switch_pm_parameter
    * @param switchPmParameter  String Optional Parameter for the start message sent to the bot when user presses the switch buttonExample: An inline bot that sends YouTube videos can ask the user to connect the bot to their YouTube account to adapt search results accordingly. To do this, it displays a 'Connect your YouTube account' button above the results, or even before showing any. The user presses the button, switches to a private chat with the bot and, in doing so, passes a start parameter that instructs the bot to return an oauth link. Once done, the bot can offer a switch_inline button so that the user can easily return to the chat where they wanted to use the bot's inline capabilities.
    */
  def answerInlineQuery(
                    results           : Seq[InlineQueryResult],
                    cacheTime         : Option[Int] = None,
                    isPersonal        : Option[Boolean] = None,
                    nextOffset        : Option[String] = None,
                    switchPmText      : Option[String] = None,
                    switchPmParameter : Option[String] = None)
                 (implicit inlineQuery: InlineQuery): Future[Boolean] = {
    request(
      AnswerInlineQuery(inlineQuery.id,results, cacheTime, isPersonal,
        nextOffset, switchPmParameter, switchPmParameter))
  }
}
