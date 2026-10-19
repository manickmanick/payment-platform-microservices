import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { requestPasswordReset, resetPassword, verifyEmail } from "../api/authApi";

export default function AccountRecoveryPage() {
  const location = useLocation(); const navigate = useNavigate();
  const token = new URLSearchParams(location.search).get("token");
  const verifyMode = location.pathname === "/verify-email";
  const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [confirm, setConfirm] = useState("");
  const [message, setMessage] = useState(""); const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  const submit = async (event) => { event.preventDefault();setBusy(true);setError("");setMessage("");try{
    if (verifyMode) { await verifyEmail(token);setMessage("Email verified. You can sign in now."); }
    else if (token) { if(password.length<8)throw new Error("Password must be at least 8 characters.");if(password!==confirm)throw new Error("Passwords do not match.");await resetPassword(token,password);setMessage("Password changed. You can sign in with your new password."); }
    else { await requestPasswordReset(email);setMessage("If an account exists, password reset instructions have been sent."); }
  }catch(e){setError(e.response?.data?.message||e.message||"Could not complete this request.");}finally{setBusy(false);} };
  return <MainLayout><section className="min-h-[70vh] bg-slate-50 px-6 py-16"><div className="mx-auto max-w-md rounded-2xl bg-white p-8 shadow"><h1 className="text-3xl font-bold">{verifyMode?"Verify email":token?"Set a new password":"Recover your account"}</h1>
    {!(verifyMode&&!token)&&<form onSubmit={submit} className="mt-6 space-y-4">{!verifyMode&&!token&&<input type="email" required value={email} onChange={(e)=>setEmail(e.target.value)} placeholder="Email address" className="w-full rounded-lg border px-3 py-2"/>}{!verifyMode&&token&&<><input type="password" required minLength={8} value={password} onChange={(e)=>setPassword(e.target.value)} placeholder="New password" className="w-full rounded-lg border px-3 py-2"/><input type="password" required minLength={8} value={confirm} onChange={(e)=>setConfirm(e.target.value)} placeholder="Confirm password" className="w-full rounded-lg border px-3 py-2"/></>}<button disabled={busy} className="w-full rounded-lg bg-orange-600 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy?"Please wait...":verifyMode?"Verify email":token?"Update password":"Send reset link"}</button></form>}
    {message&&<div role="status" className="mt-4 rounded bg-green-50 p-3 text-green-800">{message}</div>}{error&&<div role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-800">{error}</div>}
    <div className="mt-6 flex gap-4"><Link to="/login" className="text-orange-700">Sign in</Link>{!verifyMode&&<button onClick={()=>navigate("/register")} className="text-orange-700">Create account</button>}</div>
  </div></section></MainLayout>;
}
