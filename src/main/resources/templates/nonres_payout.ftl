<#ftl encoding='UTF-8'>
<#list payouts as payout>
    ${payout["party_id"]};${payout["shop_id"]};${payout["payout_id"]};${payout["sum_spis"]};${payout["fee"]};${payout["sum_poluch"]};${payout["curr"]};${payout["course"]};${payout["legal_name"]};${payout["reg_address"]};${payout["reg_num"]};${payout["bank_acc"]};${payout["bank_swift"]};${payout["bank_name"]};${payout["bank_address"]};${payout["bank_local_code"]};${payout["contract_num"]};${payout["contract_date"]};${payout["purpose"]}
</#list>