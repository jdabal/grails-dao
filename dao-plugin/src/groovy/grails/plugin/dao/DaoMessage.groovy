package grails.plugin.dao

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.transaction.interceptor.TransactionAspectSupport
import grails.validation.ValidationException
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.dao.DataAccessException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils

import java.util.Locale

/**
* A bunch of statics to support the GormDaoSupport.
*/
class DaoMessage {

	/**
	* returns a messageMap not.found.message for a not found error
	*
	* @param  id  the id of the object that was not found
	*/
	static Map notFound(domainClassName,params) {
		def domainLabel = GrailsClassUtils.getShortName(domainClassName)
		return [code:"default.not.found.message",args:[domainLabel,params.id],
			defaultMessage:"${domainLabel} not found with id ${params.id}"]
	}

	/**
	* just a short method to return a messageMap from the vals passed in
	*/
	static Map setup(messageCode,args,defaultMessage=""){
		return [code:messageCode,args:args,defaultMessage:defaultMessage]
	}
	
	/**
	* build a map of message params with ident,domainLabel and args
	* ident -> the value of the name field is the dom has one, or the id if not. 
	* domainLabel -> the label to use for the domain. just the class name unless there is a entry in message.properties
	* arg -> the array [domainLabel,ident]
	*
	* @param  entity  the domain instance to buld the message params from
	*/
	static Map buildMessageParams(entity){
		def ident = badge(entity.id,entity)
		def domainLabel = resolveDomainLabel(entity)
		def args = [domainLabel, ident]
		return [ident:ident,domainLabel:domainLabel,args:args]
	}

	static Map created(entity){
		def p = buildMessageParams(entity)
		return setup("default.created.message",p.args,"${p.domainLabel} ${p.ident} created")
	}
	
	static Map saved(entity){
		def p = buildMessageParams(entity)
		return setup("default.saved.message",p.args,"${p.domainLabel} ${p.ident} saved")
	}
	
	static Map notSaved(entity){
		def domainLabel = resolveDomainLabel(entity)
		return setup("default.not.saved.message",[domainLabel],"${domainLabel} save failed")
	}
	
	static Map updated(entity){
		def p = buildMessageParams(entity)
		return setup("default.updated.message",p.args,"${p.domainLabel} ${p.ident} updated")
	}
	
	static Map notUpdated(entity){
		def p = buildMessageParams(entity)
		return setup("default.not.updated.message",p.args, "${p.domainLabel} ${p.ident} update failed")
	}
	
	static Map deleted(entity,ident){
		def domainLabel = resolveDomainLabel(entity)
		return setup("default.deleted.message",[domainLabel,ident],"${domainLabel} ${ident} deleted")
	}
	
	static Map notDeleted(entity,ident){
		def domainLabel = resolveDomainLabel(entity)
		return setup("default.not.deleted.message",[domainLabel,ident],"${domainLabel} ${ident} could not be deleted")
	}
	static Map optimisticLockingFailure(entity){
		def domainLabel = resolveDomainLabel(entity)
		def msgMap = setup("default.optimistic.locking.failure",[domainLabel],"Another user has updated the ${domainLabel} while you were editing")
	}
	
	static String resolveDomainLabel(entity){
		return resolveMessage("${propName(entity.class.name)}.label", "${GrailsClassUtils.getShortName(entity.class.name)}")
	}
	
	static String resolveMessage(code,defaultMsg){
		return DaoUtil.ctx.messageSource.getMessage(code, [] as Object[] ,defaultMsg,  defaultLocale())
	}
	
	static Locale defaultLocale(){
		try {
			GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
			Locale currentLocale = RequestContextUtils.getLocale(webRequest.getCurrentRequest())
			return currentLocale
		}
		catch (java.lang.IllegalStateException e) {
		    return Locale.ENGLISH
		}
	}

	static String propName(String prop){
		def cname = GrailsClassUtils.getShortName(prop)
		def firstCharString = cname.charAt(0).toLowerCase().toString()
		cname = firstCharString + cname.substring(1)
		return GrailsClassUtils.getPropertyForGetter(cname)?:cname
	}

	//used for messages, if the entity has a name field then use that other wise fall back on the id and return that
	static def badge(id,entity){
		def hasName = entity?.metaClass.hasProperty(entity,'name')
		return ((hasName && entity) ? entity.name : id)
	}

}

