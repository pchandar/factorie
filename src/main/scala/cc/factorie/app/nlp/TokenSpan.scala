/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie.app.nlp
import cc.factorie._

class TokenSpan(doc:Document, initialStart:Int, initialLength:Int)(implicit d:DiffList = null) extends SpanVariable[TokenSpan,Document,Token](doc, initialStart, initialLength) with Attr {
  def document = chain // Just a convenient alias
  def sentence = this(0).sentence
  def phrase: String = if (length == 1) this.head.string else doc.string.substring(head.stringStart, last.stringEnd)
  def string: String = phrase
  /** Returns true if this span contain the words of argument span in order. */
  def containsStrings(span:TokenSpan): Boolean = {
    for (i <- 0 until length) {
      if (length - i < span.length) return false
      var result = true
      var i2 = i; var j = 0
      while (j < span.length && i2 < this.length && result) {
        if (span(j).string != this(i2)) result = false
        j += 1; i2 += 1
      }
      if (result == true) return true 
    }
    return false
  }
  override def toString = "TokenSpan("+start+":"+this.phrase+")"
  /** A short name for this span */
  def name: String = attr.values.head match {
    case label:LabelVariable[String] => label.categoryValue
    case x => x.toString
  }
  
}


// Cubbie storage




class TokenSpanCubbie extends Cubbie {
  val start = IntSlot("start")
  val length = IntSlot("length")
  def storeTokenSpan(ts:TokenSpan): this.type = {
	start := ts.start
	length := ts.length
	finishStoreTokenSpan(ts)
	this
  }
  def finishStoreTokenSpan(ts:TokenSpan): Unit = {}
  def fetchTokenSpan(doc:Document): TokenSpan = {
    val ts = new TokenSpan(doc, start.value, length.value)(null)
    finishFetchTokenSpan(ts)
    ts
  }
  def finishFetchTokenSpan(ts:TokenSpan): Unit = {}
}

trait TokenSpanWithPhraseCubbie extends TokenSpanCubbie {
  val phrase = StringSlot("phrase")
  override def finishStoreTokenSpan(ts:TokenSpan): Unit = {
    super.finishStoreTokenSpan(ts)
    phrase := ts.phrase
  }
}

trait TokenSpanWithDocRefCubbie[DC<:DocumentCubbie[_,_,_]] extends TokenSpanCubbie {
  def newDocumentCubbie: DC
  val doc = RefSlot("doc", ()=>newDocumentCubbie)
  override def finishStoreTokenSpan(ts:TokenSpan): Unit = {
    super.finishStoreTokenSpan(ts)
    doc := ts.document.name
  }
  def fetchTokenSpan(/* implicit cr:CubbieRefs */): TokenSpan = {
    throw new Error("Not yet implemented")
    val ts = new TokenSpan(null, start.value, length.value)(null)
    finishFetchTokenSpan(ts)
    ts
  }
}

trait TokenSpanNerLabelCubbieSlot extends TokenSpanCubbie {
  def newTokenSpanNerLabel(ts:TokenSpan, s:String): cc.factorie.app.nlp.ner.NerLabel
  val ner = StringSlot("ner")
  override def finishStoreTokenSpan(ts:TokenSpan): Unit = {
    super.finishStoreTokenSpan(ts)
    ner := ts.attr[cc.factorie.app.nlp.ner.NerLabel].categoryValue
  }
  override def finishFetchTokenSpan(ts:TokenSpan): Unit = {
    super.finishFetchTokenSpan(ts)
    ts.attr += newTokenSpanNerLabel(ts, ner.value)
  }
}
