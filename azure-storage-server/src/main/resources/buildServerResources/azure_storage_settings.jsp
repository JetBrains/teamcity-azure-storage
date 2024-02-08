<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>


<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
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

<l:settingsGroup title="Storage Parameters">
    <tr class="advancedSetting">
        <th class="noBorder"><label for="${params.containerName}">Container name:</label></th>
        <td>
            <div class="posRel">
                <c:set var="container" value="${propertiesBean.properties[params.containerName]}"/>
                <props:selectProperty name="${params.containerName}" className="longField">
                    <props:option value=""><c:out value="<Auto>"/></props:option>
                    <c:if test="${not empty container}">
                        <props:option value="${container}"><c:out value="${container}"/></props:option>
                    </c:if>
                </props:selectProperty>
            </div>
            <span class="smallNote">Specify the container name. If <em>&lt;Auto&gt;</em> is chosen container will be created automatically.<br/>
                You can override default path prefix via build parameter <em><c:out value="${params.pathPrefix}"/></em>
            </span>
        </td>
    </tr>
</l:settingsGroup>

<script type="text/javascript">
    function getErrors($response) {
        var $errors = $response.find("errors:eq(0) error");
        if ($errors.length) {
            return $errors.text();
        }

        return "";
    }

    function loadStorages() {
        var parameters = BS.EditStorageForm.serializeParameters();
        $j.post(window['base_uri'] + '${params.containersPath}', parameters)
            .then(function (response) {
                var $response = $j(response);
                var errors = getErrors($response);
                $j(BS.Util.escapeId('error_${params.accountKey}')).text(errors);

                if (errors) {
                    return;
                }

                var $selector = $j('#${params.containerName}');
                var value = $selector.val();
                $selector.empty().append($j("<option value=''>&lt;Auto&gt;</option>"));
                $response.find("containers:eq(0) container").map(function () {
                    var text = $j(this).text();
                    $selector.append($j("<option></option>")
                        .attr("value", text).text(text));
                });
                $selector.val(value);
            });
    }
    var selectors = BS.Util.escapeId('${params.accountName}') + ', ' + BS.Util.escapeId('${params.accountKey}');
    $j(document).on('change', selectors, function () {
        loadStorages();
    });
    $j(document).on('ready', function () {
        loadStorages();
    });
</script>