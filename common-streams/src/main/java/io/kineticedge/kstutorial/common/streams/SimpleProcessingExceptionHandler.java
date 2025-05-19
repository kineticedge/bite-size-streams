package io.kineticedge.kstutorial.common.streams;

import org.apache.kafka.streams.errors.ErrorHandlerContext;
import org.apache.kafka.streams.errors.ProcessingExceptionHandler;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SimpleProcessingExceptionHandler implements ProcessingExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(SimpleProcessingExceptionHandler.class);

  @Override
  public void configure(Map<String, ?> map) {
  }

  @Override
  public ProcessingHandlerResponse handle(ErrorHandlerContext context, Record<?, ?> record, Exception exception) {
    log.error("processorNodeId={}", context.processorNodeId(), exception);
    return ProcessingHandlerResponse.CONTINUE;
  }

}
