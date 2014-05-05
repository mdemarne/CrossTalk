package controllers.setup

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.cache.Cache
import play.api.Play.current
import views._
import models._

object Keywords extends Controller {

  val targetLanguages = List(("en", "Synonyms in English"), ("fra", "French"), ("deu", "German"), ("ru", "Russian"))

  /**
   * Contact Form definition.
   */
  val keywordsForm: Form[AllTranslations] = Form(
    mapping(
      "translations" -> seq(
        mapping(
          "targetLanguage" -> text,
          "originalKeyword" -> text,
          "keywords" -> list(text))(Translation.apply)(Translation.unapply)))(AllTranslations.apply)(AllTranslations.unapply))

  val initialInputForm: Form[InitialInput] = Form(

    mapping(
      "languages" -> list(text),
      "keywords" -> list(nonEmptyText))(InitialInput.apply)(InitialInput.unapply)
      verifying ("Please add at least one keyword!", x => !x.keywords.isEmpty))

  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.setupViews.keywordsForm(initialInputForm));
  }

  /**
   * Display a form pre-filled with an existing Contact.
   */
  def finalSubmission = Action { implicit request =>
    val resultForm = keywordsForm.bindFromRequest.get

    Cache.set("keywords", resultForm.output)

    Redirect(routes.General.viewParams)
  }

  /**
   * Handle form submission.
   */
  def initialSubmission = Action { implicit request =>

    initialInputForm.bindFromRequest.fold({ formWithErrors => BadRequest(html.setupViews.keywordsForm(initialInputForm))},{
        resultForm =>
          println(resultForm)

          val keywords = resultForm.keywords
          val translationLanguages = resultForm.languages
          //resultForm.translations.apply(1).keywords

          val startLanguage = "en"

          var submitThis = for (keyword <- keywords) yield {
            Translation("Ignore", keyword, List("Ignore"))
          }
          val tradsAndSyns =
            for (keyword <- keywords) yield {
              for (language <- translationLanguages) yield {
                val (trads, syns) = jobs.Translator(startLanguage, List(targetLanguages.apply(language.toInt)._1), keyword)()
                Translation(targetLanguages.apply(language.toInt)._2, keyword, (trads.flatten).map(_.as[String]).take(10))
              }
            }
          submitThis = submitThis.++(tradsAndSyns.flatten)
          
          Ok(html.setupViews.keywordsSummary(keywordsForm.fill(AllTranslations(submitThis))))

      })
  }
}
