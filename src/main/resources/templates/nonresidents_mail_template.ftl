<#ftl encoding='UTF-8'>
Выплаты для нерезидентов:<br>
<#list reportDescriptions as reportDescription>
${reportDescription["name"]} (ИНН ${reportDescription["inn"]}):${reportDescription["sum"]}<br>
&nbsp;&nbsp;&nbsp;(за: ${reportDescription["from_date"]}<#if reportDescription["to_date"]!=reportDescription["from_date"]> — ${reportDescription["to_date"]}</#if>; платежей: ${reportDescription["payment_sum"]}; комиссия Rbkmoney: ${reportDescription["rbk_fee_sum"]}; комиссия за вывод: ${reportDescription["fee_sum"]}<#if reportDescription["refund_sum"]??>; возвратов: ${reportDescription["refund_sum"]}</#if>)<br>
</#list>