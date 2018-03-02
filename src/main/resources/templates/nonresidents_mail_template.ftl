<#ftl encoding='UTF-8'>
Выплаты для нерезидентов:<br>
<#list reportDescriptions as reportDescription>
${reportDescription["name"]}:${reportDescription["sum"]} ${reportDescription["curr"]}, комиссия ${reportDescription["fee_sum"]} ${reportDescription["curr"]}<br>
&nbsp;&nbsp;&nbsp;(за: ${reportDescription["from_date"]}<#if reportDescription["to_date"]!=reportDescription["from_date"]> — ${reportDescription["to_date"]}</#if>; платежей: ${reportDescription["payment_sum"]} ${reportDescription["curr"]}; комиссия Rbkmoney: ${reportDescription["rbk_fee_sum"]} ${reportDescription["curr"]}<#if reportDescription["refund_sum"]??>; возвратов: ${reportDescription["refund_sum"]} ${reportDescription["curr"]}</#if>)<br>
</#list>