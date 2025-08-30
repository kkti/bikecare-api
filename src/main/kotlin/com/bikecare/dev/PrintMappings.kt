package com.bikecare.dev

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Component
class PrintMappings(
    private val mappings: List<RequestMappingHandlerMapping>
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(PrintMappings::class.java)

    override fun run(args: ApplicationArguments?) {
        mappings.forEach { handler ->
            handler.handlerMethods.forEach { (info: RequestMappingInfo, method) ->
                val methods = info.methodsCondition.methods.joinToString(",").ifEmpty { "ANY" }
                val patterns = info.pathPatternsCondition?.patternValues?.joinToString(",")
                    ?: info.patternsCondition?.patterns?.joinToString(",")
                    ?: ""
                log.info("MAPPING {} {} -> {}#{}", methods, patterns, method.beanType.simpleName, method.method.name)
            }
        }
    }
}
