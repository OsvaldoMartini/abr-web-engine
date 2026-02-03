package com.allinweb.ch.facade;

import com.allinweb.ch.model.ElementDTO;
import com.allinweb.ch.model.SplitDTO;
import com.allinweb.ch.util.ErrorMessage;
import com.allinweb.ch.util.JsScanResultDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class PerformListElements {
    private static final Logger logOperations = LoggerFactory.getLogger("com.allinweb.operations");
    private Gson gson = new GsonBuilder().create();

    private static final PerformLists performLists = PerformLists.getInstance();

    protected static volatile PerformListElements instance;
    private static JavascriptExecutor jsExecutor;
    private String jsSearchInUse =
            """
// SEARCH IN USE (SENDER: scannerTool) -> UPDATE_LIST_ELEMENTS
const __done=arguments[arguments.length-1];!function(t,e,n,a,o,i,r,l){let s=!1;function c(t){s||(s=!0,__done(JSON.stringify(t)))}setTimeout(()=>c({ok:!1,error:"timeout waiting for JS completion"}),2e4);try{"function"==typeof window.__scannerToolCleanup&&window.__scannerToolCleanup()}catch(t){}window.__scannerToolCleanup=null;const d=()=>{};window.__scannerToolCleanup=d,window.addEventListener("beforeunload",d,{once:!0}),window.elementInfoMap=new Map,window.searchTerms=t,window.allElementInfo=[],window.destination=o,window.operationId=i,window.homeBankingId=r,window.botJobId=l,window.sessionId=`${a}`;let u=!1;function m(t){if(u)return;if(["DOMContentLoaded","onreadystatechange","load","onload","Direct Execution"].includes(t)||["complete","interactive"].includes(document.readyState)){u=!0;try{g(window.searchTerms)}catch(t){c({ok:!1,error:"startCollectingElements failed",message:String(t?.message||t),stack:String(t?.stack||"")})}}}function f(t,e,n,a,o){if(0===o.length||!o.includes("with id")&&!o.includes("with name")&&!o.includes("with text")&&!o.includes("with test-id")){let o=t;for(;o&&!o.shadowRoot;)o=o.parentElement;if(o&&o.shadowRoot){const t=o.shadowRoot;h(t).forEach(i=>{x(i,a,n,e,o,t)})}else x(t,a,n,e,null,null);return}o.forEach(o=>{let i=!1;if((o.includes("with id")&&a.attributeData.some(t=>"id"===t.name)||o.includes("with name")&&a.attributeData.some(t=>"name"===t.name)||o.includes("with text")&&a.someText.length>0)&&(i=!0),i){let o=t;for(;o&&!o.shadowRoot;)o=o.parentElement;if(o&&o.shadowRoot){const t=o.shadowRoot;h(t).forEach(i=>{x(i,a,n,e,o,t)})}else x(t,a,n,e,null,null)}})}function h(t){const e=[];return["button","a"].forEach(n=>{e.push(...t.querySelectorAll(n))}),e}const p=function(t,e,n){const a=`Elements inside iframe: ${n}`;console.log(`iFrame Found: ${t.src||t.title||t.id||t.name||"No description"}; ${a}`),elementInfoMap.set(e,`xpath:${e};text:${t.src||t.title||t.id||t.name||"No description"};${a}`)},w=function e(n,a,o=!1){n.querySelectorAll("iframe").forEach(n=>{try{let o=n.contentDocument||n.contentWindow.document;if(n){let i=null,r=null;const l=C(n),s=y(n);s&&f(n,"iFrame-Found",s.xPath,s,t);const c=new DOMParser;if(n.srcdoc&&(i=c.parseFromString(n.srcdoc,"text/html"),r=i.querySelectorAll("*")),n.src){const e=function(t){try{if(!t.src)return null;if(new URL(t.src,window.location.href).origin!==window.location.origin)return null;const e=new XMLHttpRequest;if(e.open("GET",t.src,!1),e.send(),200!==e.status)return null;return(new DOMParser).parseFromString(e.responseText,"text/html").querySelectorAll("*")}catch(t){return null}}(n);e&&(p(n,l,e.length),e.forEach(function(e){const n=y(e);n&&(n.iFrameXPath=l,f(e,"iFrame-Child",`${l}${n?.xPath}`,n,t))}))}n.src||p(n,l,r?r.length:o?o.querySelectorAll("*").length:0),o.querySelectorAll("*").forEach(function(e){const n=y(e);n&&(n.iFrameXPath=l,f(e,"iFrame-Child",`${l}${n?.xPath}`,n,t))}),r?.forEach(function(e){const n=y(e);n&&(n.iFrameXPath=l,f(e,"iFrame-Child",`${l}${n?.xPath}`,n,t))}),i&&b(i,l),e(o,a,!0)}}catch(t){}})},b=function(e,n){e.querySelectorAll("*").forEach(function(e){const a=y(e);a&&(a.iFrameXPath=n,f(e,"iFrame-Child",`${n}${a?.xPath}`,a,t))})},g=function(t){window.elementInfoMap=new Map;let e=[];w(document,e,elementInfoMap),function(t,e,n){e.length>0?e.forEach(e=>{e.includes("with id")?n.push(...Array.from(t.querySelectorAll("[id]"))):e.includes("with name")?n.push(...Array.from(t.querySelectorAll("[name]"))):e.includes("with test-id")?n.push(...Array.from(t.querySelectorAll("[test-id]"))):n.push(...Array.from(t.querySelectorAll(e)))}):n.push(...Array.from(t.querySelectorAll("*")).filter(t=>"iframe"!==t.tagName.toLowerCase())),n.forEach(t=>{if(["html","body","main","script","meta","head","style"].includes(t.tagName.toLowerCase()))return;const n=y(t);n&&f(t,"tagName-Found",n.xPath,n,e)})}(document,t,e,elementInfoMap),window.allElementInfo=[],e=function(t){let e=[];return t.forEach((t,n)=>{let a=t;e.push(a)}),e}(window.elementInfoMap);const n=A(e),a=T(n),o=["input","textarea","button","a","select","label","span","div"].reduce((t,e)=>[...t,...a.filter(t=>["label","span","div"].includes(e)?t.tagName===e&&""!==t.someText?.trim():t.tagName===e)],[]);!function(t){t.forEach(t=>{if(t.attribId||t.attribName){let e=t.someText,n=t.attribId,a=t.attribName,o=null;const i=[];n&&(i.push(`label[for="${n}"] mat-label`),i.push(`mat-label[for="${n}"]`),i.push(`mat-checkbox[test-id="${a}"] .mdc-label`),i.push(`label[for="${n}"]`)),a&&(i.push(`label[for="${a}"] mat-label`),i.push(`mat-label[for="${a}"]`),i.push(`mat-checkbox[test-id="${a}"] .mdc-label`),i.push(`label[for="${n}"]`)),i.forEach(n=>{const a=document.querySelector(n);a&&null===o&&(o=a.textContent.trim(),t.attributeData.push({name:"someText",value:e}),t.someText=o)})}})}(o),function(t){t.forEach(t=>{t.someText&&"div"===t.tagName&&(t.tagName="label")})}(o),function(t){let e=1;t.forEach(t=>{window.allElementInfo.push({...t,id:e++})})}(o),window.elementInfoMap.clear(),c({ok:!0,elements:window.allElementInfo,totalElements:window.allElementInfo.length})};function x(t,e,n,a,o,i){let r="",l="",s=[];function c(t){if(!t)return"";let e=t.tagName.toLowerCase();return t.id&&(e+=`#${t.id}`),t.className&&"string"==typeof t.className&&(e+=`.${t.className.replace(/\\s+/g,".")}`),e}let d=o;for(;d;)s.unshift(c(d)),d=d.parentNode instanceof ShadowRoot?d.parentNode.host:null;o&&(r=c(o)),t&&(l=c(t));let u=l;s.length>0&&(u=s.reduceRight((t,e)=>`${e} ${t}`,l));const m={...e,shadowHost:r,shadowRoot:!!i,nestedShadow:s.length>1,cssSelector:l},f=function(t){const e=(t.tagName||"").toLowerCase(),n=t.attributeData||[],a=L(n,"label"),o=L(n,"for"),i=L(n,"id"),r=L(n,"name"),l=L(n,"aria-label"),s=L(n,"formcontrolname"),c=L(n,"test-id"),d=L(n,"data-test-id"),u=L(n,"title"),m=L(n,"value"),f=L(n,"innerhtml"),h=L(n,"href"),p=$(t.someText),w=function(t){const e=$(t);if(!e)return"";try{const t=new URL(e,window.location.href).pathname||"",n=(t.split("/").pop()||"").match(/\\.([a-z0-9]+)$/i);return n?n[1]:""}catch{const t=e.match(/\\.([a-z0-9]+)(?:[?#].*)?$/i);return t?t[1]:""}}(h),b="a"===e,g="option"===e;let x="",y="";I(a)?(x=a,y=a):I(o)?(x=o,y=o):g&&I(m)?(x=m,y=m):I(s)?(x=s,y=s):I(c)?(x=c,y=c):I(r)?(x=r,y=r):I(l)?(x=l,y=l):b&&I(f)&&!/[<>]/.test(f)?(x=f,y=f):I(i)?(x=i,y=i):I(w)?(x=`${w} File`,y=`${w} File`):I(p)?(x=p,y=e):I(d)?(x=d,y=d):I(u)?(x=u,y=u):(x=e||"",y="NO IDENTIFICATION");x=$(x),y=$(y);let E=x;(I(t.attribId)||I(t.attribName)||I(t.someText))&&(I(t.someText)?E=function(t,e){const n=$(t);return n?n.length>e?n.slice(0,e):n:""}(t.someText,30):I(t.attribId)?E=$(t.attribId):I(t.attribName)&&(E=$(t.attribName)));return{nameLabel:x,nameField:y,definedName:E}}(m)||{nameLabel:"",nameField:"",definedName:""};m&&window.elementInfoMap.set(n,S(a,m,f))}const y=function(t){if(!e&&!(0!==t.offsetWidth&&0!==t.offsetHeight&&"hidden"!==window.getComputedStyle(t).visibility||"input"===t.tagName.toLowerCase()&&"hidden"===t.type.toLowerCase()))return null;const n=Array.from(t.attributes).map(t=>({name:t.name,value:t.value})),a=t.id||"",o=t.name||"",i=`${t.getBoundingClientRect().left.toFixed(2)},${t.getBoundingClientRect().top.toFixed(2)}`;let r=t.tagName.toLowerCase();const l=function(t,e,n){let a="";if(n&&!E(n)){const t=N(n);a=[...t.titles,...t.text,...t.labels].map(t=>t.trim()).filter(Boolean).join("; ")}const o=["aria-label","aria-labelledby","aria-describedby","textarea","input","select","placeholder","label","name","title","alt","for","data-label","data-name","data-title","id","data-testid"];let i="";const r=(t,e)=>{if("aria-labelledby"===t||"aria-describedby"===t){const t=document.getElementById(e);if(t&&!E(t))return t.textContent.trim()}return e.trim()};if(a&&!/^\\..*\\{.*\\}$/.test(a))i=a;else{const t=e.find(({name:t})=>"title"===t);if(t&&(i=r(t.name,t.value)),!i)for(const t of o){const n=e.find(({name:e})=>e===t);if(n&&(i=r(n.name,n.value),i))break}}return i}(0,n,t),s=C(t),c=function(t,e){if("string"!=typeof e||""===e.trim())return"unknown";const n=e.split("/").filter(t=>""!==t.trim());for(let t=n.length-1;t>=0;t--){const e=n[t],a=e.match(/^([a-zA-Z-]+)(?:\\[\\d+\\])?/);if(!a)continue;const o=a[1].toLowerCase();if("a"===o)return"a";if("input"===o){const t=e.match(/@type=["']?([^"'\\]]+)["']?/),n=t?t[1].toLowerCase():"";return["button","submit","reset"].includes(n)?"button":"input"}if("button"===o)return"button";if(o.includes("expansion-panel-header")||o.includes("sidenav")||o.includes("nav"))return"button";if("select"===o||"option"===o)return"select";if("textarea"===o)return"input";if(o.includes("mat-button")||o.includes("mat-raised-button")||o.includes("mat-icon-button")||o.includes("mat-menu-item")||o.includes("mat-select")||o.includes("mat-option")||o.includes("matinput"))return"button";if(o.includes("data-testid")||o.includes("aria-label")||e.includes("@role='button'")||e.includes("@role='textbox'")||e.includes("react-button")||e.includes("react-link")||e.includes("react-input"))return e.includes("react-input")?"input":e.includes("react-link")?"a":"button";if(e.includes("mdc-button")||e.includes("mdc-text-field")||e.includes("mdc-list-item"))return e.includes("mdc-text-field")?"input":"button";if(e.includes("el-button")||e.includes("el-input__inner")||e.includes("el-select-dropdown__item"))return e.includes("el-input__inner")?"input":e.includes("el-select-dropdown__item")?"select":"button"}return t}(r,s);return c!==r&&(r=c),{xPath:s,tagName:r,attributeData:n,customXPath:"",attribId:a,attribName:o,coordinates:i,someText:l}},E=t=>{const e=window.getComputedStyle(t);return"none"===e.display||"hidden"===e.visibility||t.hasAttribute("aria-hidden")};function N(t){if(!t)return{text:[],labels:[],titles:[]};const e={text:new Set,labels:new Set,titles:new Set},n=t=>{const e=window.getComputedStyle(t);return!("none"===e.display||"hidden"===e.visibility||t.hasAttribute("aria-hidden"))},a=t=>t.includes("_")||t.includes("--")||t.includes("-");if(t.textContent?.trim()&&n(t)){const n=t.textContent.trim().split(/\\s+/).filter(t=>!a(t)).join(" ").trim();n&&e.text.add(n)}t.querySelectorAll("label").forEach(t=>{n(t)&&t.textContent?.trim()&&e.labels.add(t.textContent.trim());const o=t.getAttribute("for");if(o){const t=document.getElementById(o);if(t&&n(t)){const n=t.value?.trim(),o=t.placeholder?.trim();if(n){const t=n.split(/\\s+/).filter(t=>!a(t)).join(" ").trim();t&&e.text.add(t)}else if(o){const t=o.split(/\\s+/).filter(t=>!a(t)).join(" ").trim();t&&e.text.add(t)}}}});return["p","h1","h2","h3","h4","h5","h6","li","span","div","strong","em","b","i","blockquote"].forEach(o=>{t.querySelectorAll(o).forEach(t=>{if(n(t)&&t.textContent?.trim()){const n=t.textContent.trim().split(/\\s+/).filter(t=>!a(t)).join(" ").trim();n&&e.text.add(n)}})}),t.querySelectorAll("a").forEach(t=>{if(n(t)&&t.textContent?.trim()){const n=t.textContent.trim().split(/\\s+/).filter(t=>!a(t)).join(" ").trim();n&&e.text.add(n)}}),t.querySelectorAll("iframe").forEach(t=>{if(t.hasAttribute("title")){const n=t.getAttribute("title")?.trim();n&&e.titles.add(n)}try{const n=t.contentDocument||(new DOMParser).parseFromString(t.srcdoc||"","text/html");if(n.body){const t=N(n.body);t.titles.forEach(t=>e.titles.add(t)),t.text.forEach(t=>e.text.add(t)),t.labels.forEach(t=>e.labels.add(t))}}catch(t){console.log("Could not access iframe content",t)}}),{text:Array.from(e.text),labels:Array.from(e.labels),titles:Array.from(e.titles)}}const C=function t(e){if(e===document.body)return"/html/body";let n=0;const a=e.parentNode?e.parentNode.childNodes:[];for(let o=0;o<a.length;o++){let i=a[o];if(1===i.nodeType&&i.tagName===e.tagName){if(i===e)return t(e.parentNode)+"/"+e.tagName.toLowerCase()+"["+(n+1)+"]";n++}}return""};const S=function(t,e,n){return{typeElement:t,tagName:e.tagName??"No Tag Name Detected",xPath:e.xPath??"",someText:e.someText??"",attribId:e.attribId??"",attribName:e.attribName??"",coordinates:e.coordinates??"",attributeData:e.attributeData??"",customXPath:e.customXPath??"",iFrameXPath:e.iFrameXPath??"",shadowHost:e.shadowHost??"",shadowRoot:e.shadowRoot??"",nestedShadow:e.nestedShadow??"",cssSelector:e.cssSelector??"",attributeValue:e.attributeValue??"",attributeType:e.attributeType??"",searchAttributeValue:e.searchAttributeValue??"",nameLabel:n?.nameLabel??"",nameField:n?.nameField??"",definedName:n?.definedName??""}};const A=t=>{const e=new Map,n=t=>t.split("/").filter(t=>t).map(t=>{const e=t.match(/([a-zA-Z]+)(?:\\[(\\d+)\\])?/);return e?{tagName:e[1],index:e[2]?parseInt(e[2]):null}:null}).filter(t=>null!==t),a=(t,e)=>{const a=n(t),o=n(e);if(0===a.length||0===o.length)return!1;let i=0;for(let t=0;t<Math.min(a.length,o.length)&&(a[t].tagName===o[t].tagName&&a[t].index===o[t].index);t++)if("a"===a[t].tagName){i=t+1;break}return 0!==i&&a.slice(0,i).every((t,e)=>t.tagName===o[e].tagName&&t.index===o[e].index)};t.forEach(t=>{if(t.xPath&&t.coordinates){let n=!1;for(const[o,i]of e)if(a(t.xPath,o)&&t.coordinates===i[0].coordinates){i.push(t),n=!0;break}n||e.set(t.xPath,[t])}});const o=[];return e.forEach(t=>{if(t.length>1){let e=t[0];t.forEach(t=>{const[n,a]=t.coordinates.split(",").map(parseFloat),[o,i]=e.coordinates.split(",").map(parseFloat);(a>i||a===i&&n>o)&&(e=t)}),o.push(e)}else o.push(t[0])}),o},T=t=>{const e=new Map,n=new Map,a=new Map;t.forEach(t=>{if("span"!==t.tagName.toLowerCase()&&"div"!==t.tagName.toLowerCase()&&"button"!==t.tagName.toLowerCase())return;const o=t.someText?.trim();o&&o.split(/[\\s,;]+/).forEach(a=>{const o=a.trim();o&&(e.set(o,(e.get(o)||0)+1),n.has(o)||n.set(o,new Set),n.get(o).add(t))}),t.coordinates&&(a.has(t.coordinates)||a.set(t.coordinates,[]),a.get(t.coordinates).push(t))}),a.forEach(t=>{let e=t.find(t=>t.attributeData?.some(t=>"aria-label"===t.name));if(e){const n=e.attributeData.find(t=>"aria-label"===t.name);n&&t.forEach(t=>{t.someText!==n.value&&(t.someText=n.value)})}});const o=Array.from(e.entries()).filter(([t,e])=>e>1).map(([t])=>t),i=[],r=new Set,l=(t,e)=>t.attributeData?.some(t=>t.name===e);o.forEach(t=>{if(n.has(t)){let e=Array.from(n.get(t));e.sort((t,e)=>l(e,"aria-label")-l(t,"aria-label")||l(e,"test-id")-l(t,"test-id")),r.has(e[0])||(i.push(e[0]),r.add(e[0]))}}),t.forEach(t=>{if(!r.has(t)){const e=t.someText?.trim();if(e){e.split(/[\\s,;]+/).map(t=>t.trim()).some(t=>o.includes(t))||(i.push(t),r.add(t))}}});const s=new Map,c=[];i.forEach(t=>{if(t.coordinates)if(s.has(t.coordinates)){const e=s.get(t.coordinates);(!l(e,"aria-label")&&l(t,"aria-label")||t.attributeData&&e.attributeData&&t.attributeData.length>e.attributeData.length)&&(s.set(t.coordinates,t),c[c.indexOf(e)]=t)}else s.set(t.coordinates,t),c.push(t);else c.push(t)});const d=new Set,u=[];return c.forEach(t=>{t.xPath&&!d.has(t.xPath)&&(d.add(t.xPath),u.push(t))}),t.forEach(t=>{"span"!==t.tagName.toLowerCase()&&"div"!==t.tagName.toLowerCase()&&"button"!==t.tagName.toLowerCase()&&t.xPath&&!d.has(t.xPath)&&u.push(t)}),c};function $(t){return(t??"").toString().trim().replace(/\\s+/g," ")}function L(t,e){if(!Array.isArray(t))return"";const n=t.find(t=>t&&"string"==typeof t.name&&t.name.toLowerCase()===e.toLowerCase());return n?.value??""}function I(t){return $(t).length>0}window.addEventListener("message",function(t){if(t.origin===window.trustedOriginURL&&"elementsData"===t.data.type){t.data.data}}),navigator.userAgent.includes("Edg"),"complete"===document.readyState||"interactive"===document.readyState?setTimeout(()=>m("Direct Execution"),0):(document.addEventListener("DOMContentLoaded",()=>setTimeout(()=>m("DOMContentLoaded"),0)),window.addEventListener("load",()=>m("load")),document.attachEvent?.("onreadystatechange",function(){"complete"===document.readyState&&setTimeout(()=>m("onreadystatechange"),0)}),window.attachEvent?.("onload",()=>m("onload")))}(arguments[0],arguments[1],arguments[2],arguments[3],arguments[4],arguments[5],arguments[6],arguments[7]);
""";

    // Private constructor to prevent instantiation
    private PerformListElements() {}

    public static PerformListElements getInstance() {
        if (instance == null) {
            synchronized (PerformListElements.class) {
                if (instance == null) {
                    instance = new PerformListElements();
                }
            }
        }
        return instance;
    }

    // "scannerTool", "scannerGrid", "searchTerms"
    public ErrorMessage dynamicLoadElementsDTO(
            WebDriver driver,
            String[] dataArray,
            boolean searchHiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId) {

        List<String> dataList = Arrays.asList(dataArray);
        try {

            jsExecutor = (JavascriptExecutor) driver;

            driver.manage().timeouts().setScriptTimeout(java.time.Duration.ofSeconds(25));

            // "scannerTool", "scannerGrid", "searchTerms"
            Object result = jsExecutor.executeAsyncScript(
                    jsSearchInUse,
                    dataList,
                    searchHiddenFields,
                    port,
                    sessionId,
                    destination,
                    operationId,
                    homeBankingId,
                    botJobId);

            if (result == null || !(result instanceof String)) {
                logOperations.warn("Cannot return any elements from the page");
                return new ErrorMessage(
                        "Dynamic Scanner Web Page",
                        "Dynamic Load ElementsDTO error",
                        "Cannot return any elements from the page");
            }

            logOperations.info("JS async result: {}", result); // optional, but useful

            String jsonScript = String.valueOf(result);

            JsScanResultDTO dto = gson.fromJson(jsonScript, JsScanResultDTO.class);

            // Convert List<ElementDTO> -> ElementDTO[]
            SplitDTO splitDTO = new SplitDTO();
            splitDTO.setElementDetails(dto.getElements().toArray(new ElementDTO[0]));

            // Replace current list and load new one
            performLists.resetListElements();
            performLists.addElementsFromSplit(splitDTO);

            return null;
        } catch (Exception error) {
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }
}
