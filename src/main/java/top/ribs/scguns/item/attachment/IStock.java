package top.ribs.scguns.item.attachment;

import top.ribs.scguns.item.attachment.impl.Stock;

public interface IStock extends IAttachment<Stock> {
   @Override
   default IAttachment.Type getType() {
      return IAttachment.Type.STOCK;
   }
}
