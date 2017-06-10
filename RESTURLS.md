get		=>  GET /cache/key
gat		=>  GET /cache/key?cmd=gat

set		=> PUT/POST /cache/key
add		=> POST /cache/key?cmd=add
replace	=> POST /cache/key?cmd=replace
append	=> POST /cache/key?cmd=append
prepend	=> POST /cache/key?cmd=prepend
cas		=> POST /cache/key?cmd=cas

delete	=> DELETE /cache/key

incr	=> POST /cache/key?cmd=incr
decr	=> POST /cache/key?cmd=decr
touch	=> POST /cache/key?cmd=touch

stats	=> GET /stats

# http header for flags, cas, exptime, noreply; 
# http content type, content len used in storage