package top.ribs.scguns.item.attachment;

import top.ribs.scguns.item.attachment.impl.Barrel;

public interface IBarrel extends IAttachment<Barrel> {
   @Override
   default IAttachment.Type getType() {
      return IAttachment.Type.BARREL;
   }
}
