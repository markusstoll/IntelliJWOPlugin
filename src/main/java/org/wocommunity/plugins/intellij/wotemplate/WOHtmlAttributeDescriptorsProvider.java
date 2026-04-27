package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlAttributeDescriptorsProvider;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides attribute descriptors for {@code wo:*} tags in WO component HTML templates.
 *
 * In HTML files, attribute validation/completion often goes through {@link XmlAttributeDescriptorsProvider},
 * not only through {@link XmlElementDescriptor#getAttributesDescriptors(XmlTag)}.
 */
public final class WOHtmlAttributeDescriptorsProvider implements XmlAttributeDescriptorsProvider {

    @Override
    public XmlAttributeDescriptor @NotNull [] getAttributeDescriptors(@NotNull XmlTag context) {
        XmlElementDescriptor d = context.getDescriptor();
        if (!(d instanceof WOXmlElementDescriptor wod)) {
            return XmlAttributeDescriptor.EMPTY;
        }
        return wod.getAttributesDescriptors(context);
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(String attributeName, @NotNull XmlTag context) {
        XmlElementDescriptor d = context.getDescriptor();
        if (d instanceof WOXmlElementDescriptor wod) {
            return wod.getAttributeDescriptor(attributeName, context);
        }
        return null;
    }
}

