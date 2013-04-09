package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Image symbol Uses a png?
 *
 * @author Robert Toomey
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "imagesymbol")
public class ImageSymbol extends Symbol {

    @Override
    public void copyFrom(Symbol s) {
        super.copyFrom(s);
    }
}
