package com.solr98.beyondintegration.maid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.wintercogs.beyonddimensions.common.init.BDItems;

@LittleMaidExtension
public class MaidBaubleExtension implements ILittleMaid {
    @Override
    public void bindMaidBauble(BaubleManager manager) {
        manager.bind(BDItems.NET_TERMINAL_ITEM.get(), new IMaidBauble() {});
    }
}
