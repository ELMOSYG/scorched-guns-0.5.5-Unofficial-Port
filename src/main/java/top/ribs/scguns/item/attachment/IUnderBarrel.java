package top.ribs.scguns.item.attachment;

import top.ribs.scguns.item.attachment.impl.UnderBarrel;

public interface IUnderBarrel extends IAttachment<UnderBarrel> {
   @Override
   default IAttachment.Type getType() {
      return IAttachment.Type.UNDER_BARREL;
   }
}
