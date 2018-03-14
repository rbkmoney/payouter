<#ftl encoding='UTF-8'>
Выплаты для нерезидентов:<br>
<#list reportDescriptions as reportDescription>
${reportDescription["name"]}: ${reportDescription["sum"]} ${reportDescription["curr"]}<#if reportDescription["fee_sum"] != '0.00'>, комиссия ${reportDescription["fee_sum"]} ${reportDescription["curr"]}</#if><br>
&nbsp;&nbsp;&nbsp;(до: ${reportDescription["to_date_description"]}; платежей(${reportDescription["payment_count"]}): ${reportDescription["payment_sum"]} ${reportDescription["curr"]}; комиссия Rbkmoney: ${reportDescription["rbk_fee_sum"]} ${reportDescription["curr"]}<#if reportDescription["refund_sum"]??>; возвратов(${reportDescription["refund_count"]}): ${reportDescription["refund_sum"]} ${reportDescription["curr"]}</#if>)<br>
</#list>