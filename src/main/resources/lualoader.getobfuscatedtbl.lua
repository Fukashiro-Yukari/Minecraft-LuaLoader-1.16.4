-- God Damn SRG

function string.splittostrip(str,delimiter)
    local strlist, tmp = {},string.byte(delimiter)

    if delimiter == '' then
        for i = 1, #str do strlist[i] = str:sub(i,i) end
    else
        for substr in string.gmatch(str .. delimiter,'(.-)'..(((tmp > 96 and tmp < 123) or (tmp > 64 and tmp < 91) or (tmp > 47 and tmp < 58)) and delimiter or '%'..delimiter)) do
            if substr ~= '' and substr ~= ' ' then
                table.insert(strlist,substr)
            end
        end
    end
    
    return strlist
end

function string.totable( str )
	local tbl = {}

	for i = 1, string.len( str ) do
		tbl[i] = string.sub( str, i, i )
	end

	return tbl
end

local path = 'lua/obfuscated'
local mcptosrg = {}
local smt = {}
local f = io.open(path..'/McpToSrg.tsrg','r')
local lf = f:lines()
local class,oldk,oldv,oldst
local types = {
    D = 'double',
    F = 'float',
    I = 'int',
    J = 'long',
    Z = 'boolean'
}

while true do
    local s = lf()

    if !s or s == '' then
        break
    end

    if string.sub(s,1,1) != '\t' then
        class = string.splittostrip(s,' ')[1]
        class = string.gsub(class,'/','.')
        class = string.gsub(class,'%$%d%d%d%d%d','')
        class = string.gsub(class,'%$%d%d%d%d','')
        class = string.gsub(class,'%$%d%d%d','')
        class = string.gsub(class,'%$%d%d','')
        class = string.gsub(class,'%$%d','')

        mcptosrg[class] = mcptosrg[class] or {}
        oldk = nil
        oldv = nil
        oldst = nil
    else
        local s = string.sub(s,2,#s)
        local st = string.splittostrip(s,' ')
        local k = st[1]
        local v = st[#st]
        local argn = -1

        k = string.gsub(k,'%$%d%d%d%d%d','')
        k = string.gsub(k,'%$%d%d%d%d','')
        k = string.gsub(k,'%$%d%d%d','')
        k = string.gsub(k,'%$%d%d','')
        k = string.gsub(k,'%$%d','')
        k = string.gsub(k,'%$','.')
        v = string.gsub(v,'%$%d%d%d%d%d','')
        v = string.gsub(v,'%$%d%d%d%d','')
        v = string.gsub(v,'%$%d%d%d','')
        v = string.gsub(v,'%$%d%d','')
        v = string.gsub(v,'%$%d','')
        v = string.gsub(v,'%$','.')

        if #st > 2 then
            local find1 = string.find(st[2],'%(')
            local find2 = string.find(st[2],'%)')
            local args = string.sub(st[2],find1+1,find2-1)

            args = string.totable(args)

            local oargs = {}
            local isL = false
            local isadd = false

            for i = 1,#args do
                local v = args[i]

                if v == 'L' then
                    isL = true
                end

                if isL and v == ';' then
                    isL = false
                end

                if !isL then
                    if v == 'D' or v == 'F' or v == 'I' or v == 'J' or v == 'Z' then
                        isadd = true
                    end
                end

                oargs[#oargs+1] = v

                if isadd and !isL then
                    isadd = false

                    oargs[#oargs+1] = ';'
                end
            end

            args = table.concat(oargs,'')
            argn = #string.splittostrip(args,';')
        end

        mcptosrg[class][k] = {
            argn = argn,
            name = v
        }

        if oldk == k and oldv and oldst and string.find(v,'func_') == 1 and string.find(oldv,'func_') == 1 then
            smt[class] = smt[class] or {}
            
            local find1 = string.find(st[2],'%(')
            local find2 = string.find(st[2],'%)')
            local args = string.sub(st[2],find1+1,find2-1)

            args = string.totable(args)

            local oargs = {}
            local isL = false
            local isadd = false

            for i = 1,#args do
                local v = args[i]

                if v == 'L' then
                    isL = true
                end

                if isL and v == ';' then
                    isL = false
                end

                if !isL then
                    if v == 'D' or v == 'F' or v == 'I' or v == 'J' or v == 'Z' then
                        isadd = true
                    end
                end

                oargs[#oargs+1] = v

                if isadd and !isL then
                    isadd = false

                    oargs[#oargs+1] = ';'
                end
            end

            args = table.concat(oargs,'')

            local argn = #string.splittostrip(args,';')
            local argt = find2-find1

            if argt <= 1 then
                argn = 0
            end

            local find1 = string.find(oldst[2],'%(')
            local find2 = string.find(oldst[2],'%)')
            local args2 = string.sub(oldst[2],find1+1,find2-1)

            args2 = string.totable(args2)

            local oargs2 = {}
            local isL = false
            local isadd = false

            for i = 1,#args2 do
                local v = args2[i]

                if v == 'L' then
                    isL = true
                end

                if isL and v == ';' then
                    isL = false
                end

                if !isL then
                    if v == 'D' or v == 'F' or v == 'I' or v == 'J' or v == 'Z' then
                        isadd = true
                    end
                end

                oargs2[#oargs+1] = v

                if isadd and !isL then
                    isadd = false

                    oargs2[#oargs+1] = ';'
                end
            end

            args2 = table.concat(oargs2,'')

            local argn2 = #string.splittostrip(args2,';')
            local argt2 = find2-find1

            if argt2 <= 1 then
                argn2 = 0
            end

            smt[class][k] = smt[class][k] or {}

            local ttype = {}
            local ttype2 = {}

            for k,v in pairs(string.splittostrip(args,';')) do
                if types[v] then
                    v = types[v]
                end

                if string.find(v,'L') == 1 then
                    v = string.sub(v,2,#v)
                end

                v = string.gsub(v,'/','.')

                ttype[#ttype+1] = v
            end

            for k,v in pairs(string.splittostrip(args2,';')) do
                if types[v] then
                    v = types[v]
                end

                if string.find(v,'L') == 1 then
                    v = string.sub(v,2,#v)
                end

                v = string.gsub(v,'/','.')

                ttype[#ttype+1] = v
            end

            smt[class][k][#smt[class][k]+1] = {
                type = ttype,
                argn = argn,
                name = v
            }
            smt[class][k][#smt[class][k]+1] = {
                type = ttype2,
                argn = argn2,
                name = oldv
            }
        end

        oldk = k
        oldv = v
        oldst = st
    end
end

f:close()

GetTable = mcptosrg
GetFuncTable = smt