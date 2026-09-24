package top.ribs.scguns.item.attachment;

import top.ribs.scguns.item.attachment.impl.Scope;

public interface IScope extends IAttachment<Scope> {
   @Override
   default IAttachment.Type getType() {
      return IAttachment.Type.SCOPE;
   }
}
