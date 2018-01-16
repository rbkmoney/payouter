<#ftl encoding='UTF-8'>
<#list payouts as payout>
#${payout?index + 1}|PayDocRu
${date}
${payout?index + 1}
810
${payout["corr_account"]}
${payout["bik"]}
${payout["calc_account"]}
${payout["descr"]}
${payout["inn"]}
${payout["sum"]}
${payout["purpose"]}
6
${date}
01
Электронно
0
;end
</#list>