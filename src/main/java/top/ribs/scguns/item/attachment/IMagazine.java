package top.ribs.scguns.item.attachment;

import top.ribs.scguns.item.attachment.impl.Magazine;

public interface IMagazine extends IAttachment<Magazine> {
   @Override
   default IAttachment.Type getType() {
      return IAttachment.Type.MAGAZINE;
   }
}
