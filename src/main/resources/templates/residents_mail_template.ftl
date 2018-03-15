<#ftl encoding='UTF-8'>
Выплаты для резидентов:<br>
<#list reportDescriptions as reportDescription>
${reportDescription["name"]} (ИНН ${reportDescription["inn"]}): ${reportDescription["sum"]}<br>
&nbsp;&nbsp;&nbsp;(до ${reportDescription["to_date_description"]}; платежей(${reportDescription["payment_count"]}): ${reportDescription["payment_sum"]}; комиссия Rbkmoney: ${reportDescription["rbk_fee_sum"]}<#if reportDescription["fee_sum"] != '0.00'>; комиссия за вывод: ${reportDescription["fee_sum"]}</#if><#if reportDescription["refund_sum"]??>; возвратов(${reportDescription["refund_count"]}): ${reportDescription["refund_sum"]}</#if>)<br>
</#list>