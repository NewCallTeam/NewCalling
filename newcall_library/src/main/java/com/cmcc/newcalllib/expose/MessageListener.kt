package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/8/17 14:35
 */
interface MessageListener {
    /**
     * @param type msg type.
     *  [NewCallApi.MESSAGE_ON_BOOTSTRAP_READY]: report BDC created. type=0, data="". Trigger immediately on MessageListener set if BDC established and Bootstrap MiniApp ready;
     *  [NewCallApi.MESSAGE_REPORT_PARSED_BUSINESS]: report parsed business type and status. type=1, data={"phase":"precall","business":"cmrt","status":"ready"};
     * @param data message data. detail format depends on type.
     */
    fun onMessage(type: Int, data: String)
}