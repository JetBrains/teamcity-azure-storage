<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="params" class="jetbrains.buildServer.serverSide.artifacts.azure.web.AzureParametersProvider"/>

<style type="text/css">
    .runnerFormTable {
        margin-top: 1em;
    }
</style>

<l:settingsGroup title="Storage Credentials">
    <tr>
        <th class="noBorder"><label for="${params.accountName}">Account name: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${params.accountName}" className="longField"/>
            </div>
            <span class="error" id="error_${params.accountName}"></span>
            <span class="smallNote">Specify the account name.</span>
        </td>
    </tr>
    <tr>
        <th class="noBorder"><label for="${params.accountKey}">Account key: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:passwordProperty name="${params.accountKey}" className="longField"/>
            </div>
            <span class="error" id="error_${params.accountKey}"></span>
            <span class="smallNote">Specify the key to access storage account.</span>
        </td>
    </tr>
</l:settingsGroup>
