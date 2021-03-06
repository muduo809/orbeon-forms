package org.orbeon.oxf.xforms.analysis.controls

import org.dom4j.Element
import org.orbeon.oxf.xforms.analysis._
import org.orbeon.oxf.xforms.xbl.Scope

class ComponentControl(staticStateContext: StaticStateContext, element: Element, parent: Option[ElementAnalysis], preceding: Option[ElementAnalysis], scope: Scope)
        extends ContainerControl(staticStateContext, element, parent, preceding, scope) with ShadowChildrenBuilder {

    val binding = staticStateContext.partAnalysis.xblBindings.getBinding(prefixedId)

    // If control does not have an XPath binding, return one anyway so that controls w/o their own binding also get updated.
    override protected def computeBindingAnalysis =
        if (binding.abstractBinding.modeBinding) super.computeBindingAnalysis else getContextAnalysis
}