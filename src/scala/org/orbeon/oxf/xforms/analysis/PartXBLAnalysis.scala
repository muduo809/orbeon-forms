/**
 *  Copyright (C) 2007 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.analysis

import scala.collection.JavaConverters._
import collection.mutable.HashMap
import org.orbeon.oxf.common.OXFException
import org.dom4j.QName
import org.orbeon.oxf.xforms.xbl.{Scope, XBLBindings}
import org.orbeon.oxf.xforms.XFormsUtils

trait PartXBLAnalysis extends TransientState {

    self: PartAnalysisImpl ⇒

    val xblBindings = new XBLBindings(getIndentedLogger, this, metadata, staticStateDocument.xblElements)
    private[PartXBLAnalysis] val scopesById = HashMap[String, Scope]()
    private[PartXBLAnalysis] val prefixedIdToXBLScopeMap = HashMap[String, Scope]()

    protected def initializeScopes() {
        // Add existing ids to scope map
        val prefix = startScope.fullPrefix
        metadata.idGenerator.add("#document") // top-level is not added to the id generator until now
        for {
            id ← metadata.idGenerator.ids.asScala
            prefixedId = prefix + id
        } yield {
            startScope += id → prefixedId
            indexScope(prefixedId, startScope)
        }

        // Tell top-level static id generator to stop checking for duplicate ids
        // TODO: not nice, check what this is about (seems needed as of 2012-02-29)
        metadata.idGenerator.setCheckDuplicates(false)

        registerScope(startScope)
    }

    def newScope(parent: Scope, scopeId: String) =
        registerScope(new Scope(parent, scopeId))

    private def registerScope(scope: Scope) = {
        assert(! scopesById.contains(scope.scopeId))

        scopesById += scope.scopeId → scope
        scope
    }

    def indexScope(prefixedId: String, scope: Scope) {
        if (prefixedIdToXBLScopeMap.contains(prefixedId))
            throw new OXFException("Duplicate id found for prefixed id: " + prefixedId)

        prefixedIdToXBLScopeMap += prefixedId → scope
    }

    def containingScope(prefixedId: String) = {
        val prefix = XFormsUtils.getEffectiveIdPrefix(prefixedId)

        val scopeId = if (prefix.length == 0) "" else prefix.init
        scopesById.get(scopeId).orNull
    }

    def scopeForPrefixedId(prefixedId: String) =
        prefixedIdToXBLScopeMap.get(prefixedId) orNull // NOTE: only one caller tests for null: XBLContainer.findResolutionScope

    def isComponent(binding: QName) = xblBindings.isComponent(binding)
    def getBinding(prefixedId: String) = xblBindings.getBinding(prefixedId)
    def getBindingId(prefixedId: String) = xblBindings.getBindingId(prefixedId)
    def getBindingQNames = xblBindings.abstractBindings.keys toSeq
    def getAbstractBinding(binding: QName) = xblBindings.abstractBindings.get(binding)

    def getComponentBindings = xblBindings.abstractBindings

    // Search scope in ancestor or self parts
    def searchResolutionScopeByPrefixedId(prefixedId: String) =
        ancestorOrSelf map (_.scopeForPrefixedId(prefixedId)) filter (_ ne null) head

    def getGlobals = xblBindings.allGlobals

    def getXBLStyles = xblBindings.allStyles
    def getXBLScripts = xblBindings.allScripts
    def baselineResources = xblBindings.baselineResources

    override def freeTransientState() = {
        super.freeTransientState()
        xblBindings.freeTransientState()
    }
}