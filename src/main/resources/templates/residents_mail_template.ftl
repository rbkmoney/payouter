<#ftl encoding='UTF-8'>
Выплаты для резидентов: ${total_amount}<br><br>
<#list payoutDescriptions as payoutDescription>
<p style="display:inline">${payoutDescription["name"]} (ИНН ${payoutDescription["inn"]}): ${payoutDescription["sum"]} ${payoutDescription["curr"]}</p><br>
<p style="display:inline;color:gray;font-size:80%;">&nbsp;&nbsp;&nbsp;(до ${payoutDescription["to_date_description"]}; платежей(${payoutDescription["payment_count"]?c}): ${payoutDescription["payment_sum"]} ${payoutDescription["curr"]}; комиссия Rbkmoney: ${payoutDescription["rbk_fee_sum"]} ${payoutDescription["curr"]}<#if payoutDescription["fee_sum"] != '0.00'>; комиссия за вывод: ${payoutDescription["fee_sum"]} ${payoutDescription["curr"]}</#if><#if payoutDescription["refund_sum"]??>; возвратов(${payoutDescription["refund_count"]?c}): ${payoutDescription["refund_sum"]} ${payoutDescription["curr"]}</#if>)</p><br>
</#list>