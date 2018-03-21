<#ftl encoding='UTF-8'>
Выплаты для резидентов:<br><br>
<#list reportDescriptions as reportDescription>
<p style="display:inline">${reportDescription["name"]} (ИНН ${reportDescription["inn"]}): ${reportDescription["sum"]}</p><br>
<p style="display:inline;color:gray;font-size:80%;">&nbsp;&nbsp;&nbsp;(до ${reportDescription["to_date_description"]}; платежей(${reportDescription["payment_count"]?c}): ${reportDescription["payment_sum"]}; комиссия Rbkmoney: ${reportDescription["rbk_fee_sum"]}<#if reportDescription["fee_sum"] != '0.00'>; комиссия за вывод: ${reportDescription["fee_sum"]}</#if><#if reportDescription["refund_sum"]??>; возвратов(${reportDescription["refund_count"]?c}): ${reportDescription["refund_sum"]}</#if>)</p><br>
</#list>