package team.fireway.goldenlane.shared.logging;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.slf4j.MDC; import java.io.IOException; import java.util.UUID; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
@Component public class RequestIdFilter extends OncePerRequestFilter {
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException { String id=req.getHeader("X-Request-Id"); if(id==null||id.isBlank()) id="req-"+UUID.randomUUID(); MDC.put("requestId",id); res.setHeader("X-Request-Id",id); try{chain.doFilter(req,res);}finally{MDC.remove("requestId");} }
}

